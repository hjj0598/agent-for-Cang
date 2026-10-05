package com.example.demo.service.impl;

import com.example.demo.mapper.KnowledgeChunkMapper;
import com.example.demo.pojo.KnowledgeChunk;
import com.example.demo.pojo.KnowledgeDocument;
import com.example.demo.service.KnowledgeIngestService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class KnowledgeIngestServiceImpl implements KnowledgeIngestService {

    @Autowired
    private KnowledgeChunkMapper knowledgeChunkMapper;

    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Override
    public void ingest(KnowledgeDocument document) {
        List<String> chunks = splitText(document.getContent(), 500);

        for (int i = 0; i < chunks.size(); i++) {
            String chunkText = "标题：" + document.getTitle() + "\n内容：" + chunks.get(i);

            TextSegment segment = TextSegment.from(chunkText);

            Embedding embedding = embeddingModel.embed(segment).content();

            String embeddingId = embeddingStore.add(embedding, segment);

            KnowledgeChunk chunk = new KnowledgeChunk();
            chunk.setUserId(document.getUserId());
            chunk.setDocumentId(document.getId());
            chunk.setEmbeddingId(embeddingId);
            chunk.setChunkIndex(i);
            chunk.setContent(chunkText);

            knowledgeChunkMapper.insert(chunk);
        }
    }

    @Override
    public void deleteByDocument(Long documentId, Long userId) {
        List<KnowledgeChunk> chunks = knowledgeChunkMapper.findByDocumentIdAndUserId(documentId, userId);

        for (KnowledgeChunk chunk : chunks) {
            embeddingStore.remove(chunk.getEmbeddingId());
        }

        knowledgeChunkMapper.deleteByDocumentIdAndUserId(documentId, userId);
    }

    private List<String> splitText(String text, int maxLength) {
        List<String> chunks = new ArrayList<>();

        if (text == null || text.trim().isEmpty()) {
            return chunks;
        }

        String cleanText = text.trim();

        for (int start = 0; start < cleanText.length(); start += maxLength) {
            int end = Math.min(start + maxLength, cleanText.length());
            chunks.add(cleanText.substring(start, end));
        }

        return chunks;
    }
}
