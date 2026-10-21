package com.example.demo.controller;

import com.example.demo.aiservice.ConsultantService;
import com.example.demo.dto.KnowledgeHit;
import com.example.demo.dto.KnowledgeSearchResult;
import com.example.demo.dto.PersonMemoryEvidence;
import com.example.demo.dto.PersonMemoryEvidenceResult;
import com.example.demo.pojo.ChatSession;
import com.example.demo.service.ChatHistoryService;
import com.example.demo.service.KnowledgeSearchService;
import com.example.demo.service.PersonMemoryEvidenceService;
import com.example.demo.utils.AiUserContext;
import com.example.demo.utils.ChatMemoryKeyUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/ai")
public class AiChatController {

    @Autowired
    private ConsultantService consultantService;

    @Autowired
    private KnowledgeSearchService knowledgeSearchService;

    @Autowired
    private PersonMemoryEvidenceService personMemoryEvidenceService;

    @Autowired
    private ChatHistoryService chatHistoryService;

    @RequestMapping(value = "/chat", produces = "text/html;charset=utf-8")
    public Flux<String> chat(String memoryId,
                             String message,
                             HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        ChatSession session = chatHistoryService.getOrCreateSession(userId, memoryId, message);
        chatHistoryService.addMessage(userId, session.getId(), "user", message);

        KnowledgeSearchResult searchResult = safeSearchKnowledge(userId, message);
        PersonMemoryEvidenceResult memoryEvidenceResult = safeSearchPersonMemories(userId, message);
        String chatMemoryId = ChatMemoryKeyUtil.build(userId, memoryId);
        String toolContextKey = UUID.randomUUID().toString();
        AiUserContext.register(toolContextKey, userId);

        String finalMessage = """
                Server tool context key:
                %s

                User original message:
                %s

                Person memory evidence:
                %s

                Knowledge base search result:
                %s

                Rules:
                1. If the user asks you to remember, record, save, add, or store information about a person,
                   you MUST call personMemoryTool.savePersonMemory.
                2. When calling personMemoryTool, pass the exact Server tool context key above as toolContextKey.
                3. Do not pass or invent userId. The backend binds the current login user by toolContextKey.
                4. Only after the tool returns success can you say the memory has been saved.
                5. If the tool returns MEMORY_CONFLICT, explain that a conflict was detected and ask the user to confirm it in the memory page. Do not tell the user to delete old memory manually.
                6. If the tool returns MEMORY_CHANGE, explain that a possible memory change was detected and ask the user to confirm it in the memory page or memory inbox.
                   The new memory is NOT saved yet. Do NOT say it has overwritten, replaced, covered, or deleted the old memory.
                   The default confirmation behavior is to keep both old and new memories and record a temporal change relation.
                7. If the person name is missing, ask which person this memory belongs to.
                8. If Person memory evidence is relevant, use it as the user's long-term relationship memory.
                9. If Knowledge base search result is relevant, use it as document evidence.
                10. When using evidence, do not fabricate facts beyond the evidence. If evidence is insufficient, say so clearly.
                11. If person memory and knowledge base both apply, combine them carefully and explain the advice based on both.
                """.formatted(
                toolContextKey,
                message,
                memoryEvidenceResult.getContext(),
                searchResult.getContext()
        );

        Flux<String> answerFlux = consultantService.chat(chatMemoryId, finalMessage)
                .doFinally(signalType -> AiUserContext.clear(toolContextKey));

        String sourceText = buildSourceText(memoryEvidenceResult.getSources(), searchResult.getSources());
        StringBuilder assistantAnswer = new StringBuilder();

        return answerFlux.concatWith(Flux.just(sourceText))
                .doOnNext(assistantAnswer::append)
                .doOnComplete(() -> chatHistoryService.addMessage(
                        userId,
                        session.getId(),
                        "assistant",
                        assistantAnswer.toString()
                ));
    }

    private KnowledgeSearchResult safeSearchKnowledge(Long userId, String message) {
        try {
            return knowledgeSearchService.search(userId, message);
        } catch (Exception e) {
            return new KnowledgeSearchResult("知识库检索暂时不可用，本次回答不要声称引用了知识库。", new ArrayList<>());
        }
    }

    private PersonMemoryEvidenceResult safeSearchPersonMemories(Long userId, String message) {
        try {
            return personMemoryEvidenceService.search(userId, message);
        } catch (Exception e) {
            return new PersonMemoryEvidenceResult("人物记忆检索暂时不可用，本次回答不要声称引用了人物记忆。", new ArrayList<>());
        }
    }

    private String buildSourceText(List<PersonMemoryEvidence> memorySources, List<KnowledgeHit> knowledgeSources) {
        boolean hasMemorySources = memorySources != null && !memorySources.isEmpty();
        boolean hasKnowledgeSources = knowledgeSources != null && !knowledgeSources.isEmpty();

        if (!hasMemorySources && !hasKnowledgeSources) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("\n\nReference sources:\n");

        if (hasMemorySources) {
            builder.append("Person memories:\n");
            for (int i = 0; i < memorySources.size(); i++) {
                PersonMemoryEvidence source = memorySources.get(i);

                builder.append(i + 1)
                        .append(". ")
                        .append("[memory:")
                        .append(source.getMemoryId())
                        .append("] ")
                        .append(source.getPersonName())
                        .append(" / ")
                        .append(source.getMemoryType())
                        .append(" - ")
                        .append(source.getContent());

                if (source.getSourceType() != null && !source.getSourceType().trim().isEmpty()) {
                    builder.append(" (")
                            .append(source.getSourceType());

                    if (source.getSourceReference() != null && !source.getSourceReference().trim().isEmpty()) {
                        builder.append(" / ")
                                .append(source.getSourceReference());
                    }

                    builder.append(")");
                }

                builder.append(" score:")
                        .append(formatScore(source.getScore()));
                appendScore(builder, "semantic", source.getSemanticScore());
                appendScore(builder, "person", source.getPersonScore());
                appendScore(builder, "type", source.getTypeScore());
                appendScore(builder, "trust", source.getTrustScore());
                appendScore(builder, "recency", source.getRecencyScore());
                builder.append("\n");
            }
        }

        if (hasKnowledgeSources) {
            builder.append("Knowledge documents:\n");
        }

        for (int i = 0; hasKnowledgeSources && i < knowledgeSources.size(); i++) {
            KnowledgeHit source = knowledgeSources.get(i);

            builder.append(i + 1)
                    .append(". ")
                    .append("[doc:")
                    .append(source.getDocumentId())
                    .append("] ")
                    .append(source.getDocumentTitle());

            if (source.getDocumentSource() != null && !source.getDocumentSource().trim().isEmpty()) {
                builder.append(" (")
                        .append(source.getDocumentSource())
                        .append(")");
            }

            builder.append(" score:")
                    .append(formatScore(source.getScore()))
                    .append("\n");
        }

        return builder.toString();
    }

    private void appendScore(StringBuilder builder, String name, Double score) {
        if (score == null) {
            return;
        }

        builder.append(" ")
                .append(name)
                .append(":")
                .append(formatScore(score));
    }

    private String formatScore(Double score) {
        if (score == null) {
            return "0.00";
        }

        return String.format(Locale.US, "%.2f", score);
    }
}
