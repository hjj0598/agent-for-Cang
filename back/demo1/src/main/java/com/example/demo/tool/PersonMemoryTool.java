package com.example.demo.tool;

import com.example.demo.dto.MemoryConflictResult;
import com.example.demo.dto.MemoryCandidateCreateRequest;
import com.example.demo.mapper.PersonMapper;
import com.example.demo.pojo.MemoryCandidateRecord;
import com.example.demo.pojo.Person;
import com.example.demo.pojo.PersonMemory;
import com.example.demo.service.MemoryCandidateService;
import com.example.demo.service.PersonMemoryService;
import com.example.demo.utils.AiUserContext;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component("personMemoryTool")
public class PersonMemoryTool {

    @Autowired
    private PersonMemoryService personMemoryService;

    @Autowired
    private PersonMapper personMapper;

    @Autowired
    private MemoryCandidateService memoryCandidateService;

    @Tool("Query a person's profile and memories. You must pass the server issued toolContextKey from the current message.")
    public String queryPersonMemories(@P("server issued tool context key") String toolContextKey,
                                      @P("person name") String personName) {
        Long userId = AiUserContext.requireUserId(toolContextKey);
        Person person = personMapper.findByUserIdAndName(userId, personName);
        List<PersonMemory> memories = personMemoryService.findByUserIdAndPersonName(userId, personName);

        if (person == null && memories.isEmpty()) {
            return "No information found for " + personName + ".";
        }

        StringBuilder result = new StringBuilder();

        if (person != null) {
            result.append("personName: ").append(person.getName()).append("\n");

            if (person.getRelation() != null && !person.getRelation().trim().isEmpty()) {
                result.append("relation: ").append(person.getRelation()).append("\n");
            }

            if (person.getDescription() != null && !person.getDescription().trim().isEmpty()) {
                result.append("description: ").append(person.getDescription()).append("\n");
            }
        }

        if (!memories.isEmpty()) {
            result.append("memories:\n");
            result.append(memories.stream()
                    .map(memory -> {
                        String source = memory.getSourceType();
                        if (source == null || source.trim().isEmpty()) {
                            source = memory.getSource();
                        }

                        String reference = memory.getSourceReference();
                        String sourceText = source == null || source.trim().isEmpty()
                                ? ""
                                : " [来源：" + source
                                + (reference == null || reference.trim().isEmpty()
                                ? ""
                                : " / " + reference)
                                + "]";

                        return memory.getMemoryType() + ": " + memory.getContent() + sourceText;
                    })
                    .collect(Collectors.joining("\n")));
        } else {
            result.append("memories: none");
        }

        return result.toString();
    }

    @Tool("Save one memory for a person. You must pass the server issued toolContextKey from the current message. If the person does not exist, create the person automatically.")
    public String savePersonMemory(@P("server issued tool context key") String toolContextKey,
                                   @P("person name") String personName,
                                   @P("memory type, must be one of HOBBY, QUOTE, TRAIT, DISLIKE, DATE, NOTE") String memoryType,
                                   @P("memory content") String content) {
        Long userId = AiUserContext.requireUserId(toolContextKey);
        MemoryConflictResult result = personMemoryService.addByUserIdAndPersonName(userId, personName, memoryType, content, "AI chat");

        if ("CONFLICT".equals(result.getStatus())) {
            return "MEMORY_CONFLICT: " + result.getReason()
                    + " 请告诉用户这条记忆可能与旧记忆冲突，需要到人物详情页确认后再保存。";
        }
        if ("CHANGE".equals(result.getStatus())) {
            return "MEMORY_CHANGE: " + result.getReason()
                    + " 请告诉用户这条新记忆尚未保存，需要到人物详情页或记忆收件箱确认。"
                    + " 默认确认方式是保留新旧两条记忆并记录时间变化关系，不要说覆盖、替换或删除旧记忆。";
        }

        return "SAVE_SUCCESS: " + personName + " - " + memoryType + " - " + content;
    }

    @Tool("Create a candidate memory for user review instead of saving it directly. You must pass the server issued toolContextKey from the current message.")
    public String createPersonMemoryCandidate(@P("server issued tool context key") String toolContextKey,
                                              @P("person name") String personName,
                                              @P("memory type, must be one of HOBBY, QUOTE, TRAIT, DISLIKE, DATE, NOTE") String memoryType,
                                              @P("memory content") String content,
                                              @P("confidence from 0 to 1") Double confidence,
                                              @P("fact type, such as USER_CONFIRMED_FACT, DIRECT_QUOTE, USER_OPINION, AI_EXTRACTED_FACT, AI_INFERENCE, THIRD_PARTY_STATEMENT") String factType) {
        Long userId = AiUserContext.requireUserId(toolContextKey);

        MemoryCandidateCreateRequest request = new MemoryCandidateCreateRequest();
        request.setPersonName(personName);
        request.setMemoryType(memoryType);
        request.setContent(content);
        request.setSourceType("AI 对话");
        request.setSourceReference("聊天 Tool 提取");
        request.setConfidence(confidence);
        request.setFactType(factType);

        MemoryCandidateRecord candidate = memoryCandidateService.create(userId, request);
        return "CANDIDATE_CREATED: candidateId=" + candidate.getId()
                + ", personName=" + personName
                + ", memoryType=" + candidate.getMemoryType();
    }
}
