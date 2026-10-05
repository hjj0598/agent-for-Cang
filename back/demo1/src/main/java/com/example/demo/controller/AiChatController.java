package com.example.demo.controller;

import com.example.demo.aiservice.ConsultantService;
import com.example.demo.dto.KnowledgeHit;
import com.example.demo.dto.KnowledgeSearchResult;
import com.example.demo.pojo.ChatSession;
import com.example.demo.service.ChatHistoryService;
import com.example.demo.service.KnowledgeSearchService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/ai")
public class AiChatController {

    @Autowired
    private ConsultantService consultantService;

    @Autowired
    private KnowledgeSearchService knowledgeSearchService;

    @Autowired
    private ChatHistoryService chatHistoryService;

    @RequestMapping(value = "/chat", produces = "text/html;charset=utf-8")
    public Flux<String> chat(String memoryId,
                             String message,
                             HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        ChatSession session = chatHistoryService.getOrCreateSession(userId, memoryId, message);
        chatHistoryService.addMessage(userId, session.getId(), "user", message);

        KnowledgeSearchResult searchResult = knowledgeSearchService.search(userId, message);

        String finalMessage = """
                Current login userId:
                %s

                User original message:
                %s

                Knowledge base search result:
                %s

                Rules:
                1. If the user asks you to remember, record, save, add, or store information about a person,
                   you MUST call personMemoryTool.savePersonMemory.
                2. When saving memory, use the Current login userId above as the userId argument.
                3. Only after the tool returns success can you say the memory has been saved.
                4. If the person name is missing, ask which person this memory belongs to.
                5. If the user asks a question and the knowledge base result is relevant, answer using it first.
                """.formatted(userId, message, searchResult.getContext());

        Flux<String> answerFlux = consultantService.chat(memoryId, finalMessage);

        String sourceText = buildSourceText(searchResult.getSources());
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

    private String buildSourceText(List<KnowledgeHit> sources) {
        if (sources == null || sources.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("\n\nReference sources:\n");

        for (int i = 0; i < sources.size(); i++) {
            KnowledgeHit source = sources.get(i);

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
                    .append(String.format("%.2f", source.getScore()))
                    .append("\n");
        }

        return builder.toString();
    }
}
