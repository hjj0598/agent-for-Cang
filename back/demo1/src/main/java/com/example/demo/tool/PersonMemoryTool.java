package com.example.demo.tool;

import com.example.demo.mapper.PersonMapper;
import com.example.demo.pojo.Person;
import com.example.demo.pojo.PersonMemory;
import com.example.demo.service.PersonMemoryService;
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

    @Tool("Query a person's profile and memories by current login userId and person name.")
    public String queryPersonMemories(@P("current login userId") Long userId,
                                      @P("person name") String personName) {
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

    @Tool("Save one memory for a person by current login userId. If the person does not exist, create the person automatically.")
    public String savePersonMemory(@P("current login userId") Long userId,
                                   @P("person name") String personName,
                                   @P("memory type, must be one of HOBBY, QUOTE, TRAIT, DISLIKE, DATE, NOTE") String memoryType,
                                   @P("memory content") String content) {
        personMemoryService.addByUserIdAndPersonName(userId, personName, memoryType, content, "AI chat");

        return "SAVE_SUCCESS: " + personName + " - " + memoryType + " - " + content;
    }
}
