package com.example.demo.service.impl;

import com.example.demo.dto.KnowledgeHit;
import com.example.demo.dto.KnowledgeSearchResult;
import com.example.demo.mapper.KnowledgeChunkMapper;
import com.example.demo.service.KnowledgeSearchService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class KnowledgeSearchServiceImpl implements KnowledgeSearchService {

    private static final int FINAL_RESULT_LIMIT = 5;
    private static final int USER_FILTER_RECALL_LIMIT = 50;

    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private KnowledgeChunkMapper knowledgeChunkMapper;

    @Override
    public KnowledgeSearchResult search(Long userId, String question) {
        Embedding queryEmbedding = embeddingModel.embed(question).content();

        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                /*
                 * Redis 向量检索本身是全局召回，随后还要按 userId 从 MySQL
                 * 校验归属。先多召回一些，再过滤并截取最终结果，避免其他用户
                 * 的向量占满前 5 条导致当前用户没有结果。
                 */
                .maxResults(USER_FILTER_RECALL_LIMIT)
                .minScore(0.75)
                .build();

        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);

        List<String> contents = new ArrayList<>();
        Map<Long, KnowledgeHit> sourceMap = new LinkedHashMap<>();

        for (EmbeddingMatch<TextSegment> match : result.matches()) {
            if (contents.size() >= FINAL_RESULT_LIMIT) {
                break;
            }

            KnowledgeHit hit = knowledgeChunkMapper.findHitByEmbeddingIdAndUserId(match.embeddingId(), userId);

            if (hit == null) {
                continue;
            }

            hit.setScore(match.score());
            contents.add(hit.getContent());

            if (!sourceMap.containsKey(hit.getDocumentId())) {
                sourceMap.put(hit.getDocumentId(), hit);
            }
        }

        if (contents.isEmpty()) {
            return new KnowledgeSearchResult("未检索到相关知识库内容。", new ArrayList<>());
        }

        return new KnowledgeSearchResult(String.join("\n\n", contents), new ArrayList<>(sourceMap.values()));
    }
}
