package com.example.demo.service.impl;

import com.example.demo.dto.PersonMemoryEvidence;
import com.example.demo.dto.PersonMemoryEvidenceResult;
import com.example.demo.mapper.PersonMapper;
import com.example.demo.mapper.PersonMemoryEmbeddingMapper;
import com.example.demo.mapper.PersonMemoryMapper;
import com.example.demo.pojo.Person;
import com.example.demo.service.PersonMemoryEmbeddingService;
import com.example.demo.service.PersonMemoryEvidenceService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class PersonMemoryEvidenceServiceImpl implements PersonMemoryEvidenceService {

    private static final int CANDIDATE_LIMIT = 300;
    private static final int VECTOR_RECALL_LIMIT = 80;
    private static final int FINAL_LIMIT = 5;

    private static final double SEMANTIC_WEIGHT = 0.50D;
    private static final double PERSON_WEIGHT = 0.20D;
    private static final double TYPE_WEIGHT = 0.10D;
    private static final double TRUST_WEIGHT = 0.10D;
    private static final double RECENCY_WEIGHT = 0.10D;

    @Autowired
    private PersonMemoryMapper personMemoryMapper;

    @Autowired
    private PersonMemoryEmbeddingMapper personMemoryEmbeddingMapper;

    @Autowired
    private PersonMapper personMapper;

    @Autowired
    private PersonMemoryEmbeddingService personMemoryEmbeddingService;

    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Override
    public PersonMemoryEvidenceResult search(Long userId, String question) {
        List<PersonMemoryEvidence> allMemories = personMemoryMapper.findEvidenceByUserId(userId, CANDIDATE_LIMIT);
        if (allMemories == null || allMemories.isEmpty()) {
            return new PersonMemoryEvidenceResult("未检索到相关人物记忆。", new ArrayList<>());
        }

        allMemories.forEach(this::enrichSource);
        personMemoryEmbeddingService.indexMissing(userId, allMemories);

        List<String> mentionedNames = extractPersonNames(userId, question);
        Map<Long, PersonMemoryEvidence> candidateMap = new LinkedHashMap<>();

        for (PersonMemoryEvidence memory : searchByVector(userId, question)) {
            candidateMap.put(memory.getMemoryId(), memory);
        }

        if (!mentionedNames.isEmpty()) {
            for (PersonMemoryEvidence memory : allMemories) {
                if (personMatchScore(memory, mentionedNames) > 0D) {
                    candidateMap.putIfAbsent(memory.getMemoryId(), memory);
                }
            }
        }

        if (candidateMap.isEmpty()) {
            for (PersonMemoryEvidence memory : allMemories) {
                double lexicalScore = lexicalSemanticFallback(question, memory.getContent());
                if (lexicalScore > 0D || personMatchScore(memory, mentionedNames) > 0D) {
                    memory.setSemanticScore(lexicalScore);
                    candidateMap.put(memory.getMemoryId(), memory);
                }
            }
        }

        List<PersonMemoryEvidence> scored = candidateMap.values().stream()
                .map(memory -> scoreMemory(memory, question, mentionedNames))
                .filter(memory -> memory.getScore() != null && memory.getScore() > 0.18D)
                .sorted(Comparator.comparing(PersonMemoryEvidence::getScore).reversed()
                        .thenComparing(PersonMemoryEvidence::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(FINAL_LIMIT)
                .toList();

        if (scored.isEmpty()) {
            return new PersonMemoryEvidenceResult("未检索到相关人物记忆。", new ArrayList<>());
        }

        return new PersonMemoryEvidenceResult(buildContext(scored), scored);
    }

    private List<PersonMemoryEvidence> searchByVector(Long userId, String question) {
        List<PersonMemoryEvidence> results = new ArrayList<>();
        if (question == null || question.trim().isEmpty()) {
            return results;
        }

        try {
            Embedding queryEmbedding = embeddingModel.embed(question).content();
            EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(VECTOR_RECALL_LIMIT)
                    .minScore(0.60D)
                    .build();

            EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(request);
            for (EmbeddingMatch<TextSegment> match : searchResult.matches()) {
                PersonMemoryEvidence memory = personMemoryEmbeddingMapper.findEvidenceByEmbeddingIdAndUserId(match.embeddingId(), userId);
                if (memory == null) {
                    continue;
                }

                memory.setSemanticScore(clamp(match.score()));
                enrichSource(memory);
                results.add(memory);
            }
        } catch (Exception ignored) {
            return new ArrayList<>();
        }

        return results;
    }

    private PersonMemoryEvidence scoreMemory(PersonMemoryEvidence memory,
                                             String question,
                                             List<String> mentionedNames) {
        double semanticScore = memory.getSemanticScore() == null
                ? lexicalSemanticFallback(question, memory.getContent())
                : clamp(memory.getSemanticScore());
        double personScore = personMatchScore(memory, mentionedNames);
        double typeScore = typeMatchScore(question, memory.getMemoryType());
        double trustScore = trustScore(memory);
        double recencyScore = recencyScore(memory.getUpdatedAt());

        double finalScore = semanticScore * SEMANTIC_WEIGHT
                + personScore * PERSON_WEIGHT
                + typeScore * TYPE_WEIGHT
                + trustScore * TRUST_WEIGHT
                + recencyScore * RECENCY_WEIGHT;

        memory.setSemanticScore(semanticScore);
        memory.setPersonScore(personScore);
        memory.setTypeScore(typeScore);
        memory.setTrustScore(trustScore);
        memory.setRecencyScore(recencyScore);
        memory.setScore(clamp(finalScore));
        return memory;
    }

    private List<String> extractPersonNames(Long userId, String question) {
        String normalizedQuestion = normalize(question);
        if (normalizedQuestion.isEmpty()) {
            return new ArrayList<>();
        }

        return personMapper.findByUserId(userId).stream()
                .map(Person::getName)
                .filter(name -> {
                    String normalizedName = normalize(name);
                    return !normalizedName.isEmpty() && normalizedQuestion.contains(normalizedName);
                })
                .toList();
    }

    private double personMatchScore(PersonMemoryEvidence memory, List<String> mentionedNames) {
        if (mentionedNames == null || mentionedNames.isEmpty()) {
            return 0.2D;
        }

        String personName = normalize(memory.getPersonName());
        for (String name : mentionedNames) {
            String normalizedName = normalize(name);
            if (personName.equals(normalizedName)) {
                return 1.0D;
            }
            if (personName.contains(normalizedName) || normalizedName.contains(personName)) {
                return 0.7D;
            }
        }

        return 0D;
    }

    private double typeMatchScore(String question, String memoryType) {
        String q = normalize(question);
        String type = memoryType == null ? "NOTE" : memoryType.trim().toUpperCase(Locale.ROOT);

        if (q.contains("说过") || q.contains("原话") || q.contains("他说") || q.contains("她说")) {
            return "QUOTE".equals(type) ? 1.0D : 0.3D;
        }

        if (q.contains("喜欢") || q.contains("爱好") || q.contains("偏好")) {
            return "HOBBY".equals(type) ? 1.0D : 0.4D;
        }

        if (q.contains("讨厌") || q.contains("不喜欢") || q.contains("雷点")) {
            return "DISLIKE".equals(type) ? 1.0D : 0.4D;
        }

        if (q.contains("性格") || q.contains("特点") || q.contains("风格")) {
            return "TRAIT".equals(type) ? 1.0D : 0.4D;
        }

        if (q.contains("生日") || q.contains("纪念日") || q.contains("日期") || q.contains("什么时候")) {
            return "DATE".equals(type) ? 1.0D : 0.2D;
        }

        if (q.contains("怎么相处") || q.contains("怎么聊") || q.contains("建议") || q.contains("沟通") || q.contains("鼓励")) {
            if ("TRAIT".equals(type) || "HOBBY".equals(type) || "DISLIKE".equals(type)) {
                return 0.8D;
            }
            if ("QUOTE".equals(type)) {
                return 0.6D;
            }
            return 0.4D;
        }

        return "NOTE".equals(type) ? 0.5D : 0.6D;
    }

    private double trustScore(PersonMemoryEvidence memory) {
        String source = memory.getSource() == null ? "" : memory.getSource();
        String type = memory.getMemoryType() == null ? "" : memory.getMemoryType().toUpperCase(Locale.ROOT);

        if ("QUOTE".equals(type)) {
            return 0.9D;
        }
        if (source.startsWith("手动记录")) {
            return 1.0D;
        }
        if (source.startsWith("图片提取")) {
            return 0.75D;
        }
        if (source.contains("AI聊天") || source.contains("AI chat") || source.contains("AI 对话")) {
            return 0.65D;
        }
        if (source.startsWith("知识库")) {
            return 0.8D;
        }

        return 0.7D;
    }

    private double recencyScore(LocalDateTime updatedAt) {
        if (updatedAt == null) {
            return 0.3D;
        }

        long days = ChronoUnit.DAYS.between(updatedAt, LocalDateTime.now());
        if (days <= 30) {
            return 1.0D;
        }
        if (days <= 90) {
            return 0.8D;
        }
        if (days <= 180) {
            return 0.6D;
        }
        if (days <= 365) {
            return 0.4D;
        }
        return 0.2D;
    }

    private double lexicalSemanticFallback(String question, String content) {
        String q = normalize(question);
        String c = normalize(content);
        if (q.isEmpty() || c.isEmpty()) {
            return 0D;
        }

        double score = 0D;
        if (c.contains(q) || q.contains(c)) {
            score += 0.6D;
        }

        double overlap = overlapScore(q, c) / 6D;
        score += Math.min(overlap, 0.4D);
        return clamp(score);
    }

    private double overlapScore(String question, String content) {
        double score = 0D;
        for (String gram : grams(question)) {
            if (content.contains(gram)) {
                score += gram.length() >= 3 ? 1.4D : 0.7D;
            }
        }
        return Math.min(score, 6D);
    }

    private List<String> grams(String text) {
        List<String> grams = new ArrayList<>();
        if (text == null || text.length() < 2) {
            return grams;
        }

        int maxGramLength = Math.min(4, text.length());
        for (int length = maxGramLength; length >= 2; length--) {
            for (int index = 0; index + length <= text.length(); index++) {
                String gram = text.substring(index, index + length);
                if (!isNoise(gram)) {
                    grams.add(gram);
                }
            }
        }
        return grams;
    }

    private boolean isNoise(String text) {
        return text.matches("[的是了呢吗啊吧和或者以及一个什么怎么如何根据关于]+");
    }

    private String buildContext(List<PersonMemoryEvidence> memories) {
        StringBuilder context = new StringBuilder();
        for (PersonMemoryEvidence memory : memories) {
            context.append("- memoryId: ").append(memory.getMemoryId()).append("\n")
                    .append("  person: ").append(valueOrUnknown(memory.getPersonName()));
            if (memory.getRelation() != null && !memory.getRelation().trim().isEmpty()) {
                context.append(" / ").append(memory.getRelation().trim());
            }
            context.append("\n")
                    .append("  type: ").append(valueOrUnknown(memory.getMemoryType())).append("\n")
                    .append("  content: ").append(valueOrUnknown(memory.getContent())).append("\n")
                    .append("  source: ").append(valueOrUnknown(memory.getSourceType()));
            if (memory.getSourceReference() != null && !memory.getSourceReference().trim().isEmpty()) {
                context.append(" / ").append(memory.getSourceReference().trim());
            }
            context.append("\n")
                    .append("  finalScore: ").append(format(memory.getScore()))
                    .append(" semantic: ").append(format(memory.getSemanticScore()))
                    .append(" person: ").append(format(memory.getPersonScore()))
                    .append(" type: ").append(format(memory.getTypeScore()))
                    .append(" trust: ").append(format(memory.getTrustScore()))
                    .append(" recency: ").append(format(memory.getRecencyScore()))
                    .append("\n");
        }
        return context.toString();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[\\s，。！？、,.!?;；:：\"'“”‘’()（）\\[\\]【】<>《》]", "")
                .trim();
    }

    private void enrichSource(PersonMemoryEvidence memory) {
        String source = memory.getSource();
        if (source == null || source.trim().isEmpty()) {
            memory.setSource("手动记录");
            memory.setSourceType("手动记录");
            memory.setSourceReference("");
            return;
        }

        String cleanSource = source.trim();
        String sourceType = "手动记录";
        String sourceReference = cleanSource;

        if (cleanSource.startsWith("图片提取")) {
            sourceType = "图片提取";
            sourceReference = afterSeparator(cleanSource);
        } else if (cleanSource.contains("AI聊天") || cleanSource.contains("AI chat") || cleanSource.contains("AI 对话")) {
            sourceType = "AI 对话";
            sourceReference = afterSeparator(cleanSource);
        } else if (cleanSource.startsWith("知识库")) {
            sourceType = "知识库";
            sourceReference = afterSeparator(cleanSource);
        }

        memory.setSourceType(sourceType);
        memory.setSourceReference(sourceReference);
    }

    private String afterSeparator(String source) {
        int chineseSeparator = source.indexOf('：');
        int asciiSeparator = source.indexOf(':');
        int separator = chineseSeparator >= 0 ? chineseSeparator : asciiSeparator;

        if (separator < 0 || separator + 1 >= source.length()) {
            return "";
        }

        return source.substring(separator + 1).trim();
    }

    private double clamp(Double value) {
        if (value == null || value.isNaN()) {
            return 0D;
        }
        return Math.max(0D, Math.min(1D, value));
    }

    private String format(Double value) {
        return String.format(Locale.ROOT, "%.2f", clamp(value));
    }

    private String valueOrUnknown(String value) {
        return value == null || value.trim().isEmpty() ? "未记录" : value.trim();
    }
}
