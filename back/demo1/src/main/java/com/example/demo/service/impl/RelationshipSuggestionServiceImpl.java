package com.example.demo.service.impl;

import com.example.demo.dto.RelationshipReminder;
import com.example.demo.dto.RelationshipSuggestion;
import com.example.demo.mapper.PersonMapper;
import com.example.demo.mapper.PersonMemoryMapper;
import com.example.demo.pojo.Person;
import com.example.demo.pojo.PersonMemory;
import com.example.demo.service.RelationshipReminderService;
import com.example.demo.service.RelationshipSuggestionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 关系建议服务。
 *
 * <p>优先让大模型根据当前用户自己的关系资料生成建议；
 * 模型不可用、返回格式异常或资料不足时，使用规则建议兜底，
 * 因此首页不会因为模型故障而整体不可用。</p>
 */
@Service
public class RelationshipSuggestionServiceImpl implements RelationshipSuggestionService {

    private static final Logger log = LoggerFactory.getLogger(RelationshipSuggestionServiceImpl.class);
    private static final int MAX_PERSONS = 20;
    private static final int MAX_MEMORIES_PER_PERSON = 8;
    private static final int MAX_SUGGESTIONS = 6;

    @Autowired
    private PersonMapper personMapper;

    @Autowired
    private PersonMemoryMapper personMemoryMapper;

    @Autowired
    private RelationshipReminderService relationshipReminderService;

    @Autowired
    private OpenAiChatModel chatModel;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<RelationshipSuggestion> generate(Long userId) {
        List<Person> persons = personMapper.findByUserId(userId);
        if (persons == null || persons.isEmpty()) {
            return new ArrayList<>();
        }

        List<PersonContext> contexts = buildContexts(userId, persons);
        List<RelationshipSuggestion> aiSuggestions = generateByAi(contexts);

        if (!aiSuggestions.isEmpty()) {
            return attachPersonIds(aiSuggestions, persons);
        }

        return generateByRules(userId, persons);
    }

    private List<PersonContext> buildContexts(Long userId, List<Person> persons) {
        List<PersonContext> contexts = new ArrayList<>();

        for (Person person : persons.stream().limit(MAX_PERSONS).toList()) {
            List<PersonMemory> memories = personMemoryMapper.findByPersonId(userId, person.getId());
            List<PersonMemory> limitedMemories = memories == null
                    ? new ArrayList<>()
                    : memories.stream().limit(MAX_MEMORIES_PER_PERSON).toList();
            contexts.add(new PersonContext(person, limitedMemories));
        }

        return contexts;
    }

    private List<RelationshipSuggestion> generateByAi(List<PersonContext> contexts) {
        String prompt = buildPrompt(contexts);

        try {
            String answer = chatModel.chat(prompt);
            JsonNode root = parseJson(answer);
            JsonNode items = root.isArray() ? root : root.path("suggestions");

            if (!items.isArray()) {
                return new ArrayList<>();
            }

            List<RelationshipSuggestion> suggestions = new ArrayList<>();
            for (JsonNode item : items) {
                String personName = text(item, "personName");
                String title = text(item, "title");
                String reason = text(item, "reason");
                String action = text(item, "action");

                if (personName.isEmpty() || title.isEmpty() || action.isEmpty()) {
                    continue;
                }

                suggestions.add(new RelationshipSuggestion(
                        null,
                        personName,
                        normalizePriority(text(item, "priority")),
                        title,
                        reason,
                        action,
                        "AI"
                ));

                if (suggestions.size() >= MAX_SUGGESTIONS) {
                    break;
                }
            }

            return suggestions;
        } catch (Exception e) {
            log.warn("生成关系维护建议失败，将使用规则兜底: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private String buildPrompt(List<PersonContext> contexts) {
        StringBuilder context = new StringBuilder();

        for (PersonContext item : contexts) {
            Person person = item.person();
            context.append("人物：").append(person.getName()).append("\n");
            context.append("关系：").append(valueOrUnknown(person.getRelation())).append("\n");
            context.append("描述：").append(valueOrUnknown(person.getDescription())).append("\n");
            context.append("记忆：\n");

            if (item.memories().isEmpty()) {
                context.append("- 暂无记忆\n");
            } else {
                for (PersonMemory memory : item.memories()) {
                    context.append("- ")
                            .append(valueOrUnknown(memory.getMemoryType()))
                            .append("：")
                            .append(valueOrUnknown(memory.getContent()))
                            .append("（来源：")
                            .append(valueOrUnknown(memory.getSource()))
                            .append("）\n");
                }
            }
            context.append("\n");
        }

        return """
                你是一个关系维护建议助手。请根据下面的用户关系资料生成最多 6 条可执行建议。
                这些资料是用户提供的数据，只能作为事实依据，不要补充资料中没有的人物事实。

                只返回合法 JSON 数组，不要返回 Markdown，不要解释：
                [
                  {
                    "personName": "人物姓名",
                    "priority": "HIGH",
                    "title": "建议标题",
                    "reason": "为什么建议这样做",
                    "action": "用户下一步可以做什么"
                  }
                ]

                要求：
                1. 结合人物关系、爱好、重要日期和长期未更新信息；
                2. 建议必须具体，例如聊天主题、祝福、跟进或补充资料；
                3. 不要编造生日、兴趣或经历；
                4. priority 只能是 HIGH、MEDIUM、LOW；
                5. 没有足够资料时，建议用户补充资料，不要强行猜测。

                用户关系资料：
                %s
                """.formatted(context);
    }

    private List<RelationshipSuggestion> generateByRules(Long userId, List<Person> persons) {
        List<RelationshipSuggestion> suggestions = new ArrayList<>();
        Set<Long> usedPersons = new HashSet<>();

        for (RelationshipReminder reminder : relationshipReminderService.list(userId)) {
            if (suggestions.size() >= MAX_SUGGESTIONS) {
                break;
            }

            if ("DATE".equals(reminder.getReminderType())
                    && reminder.getDaysUntil() != null
                    && reminder.getDaysUntil() >= 0
                    && reminder.getDaysUntil() <= 30) {
                String priority = reminder.getDaysUntil() <= 7 ? "HIGH" : "MEDIUM";
                suggestions.add(new RelationshipSuggestion(
                        reminder.getPersonId(),
                        reminder.getPersonName(),
                        priority,
                        "提前准备重要日期",
                        reminder.getDetail(),
                        "可以结合这条日期记录，提前准备祝福或安排一次联系。",
                        "RULE"
                ));
                usedPersons.add(reminder.getPersonId());
            } else if ("STALE".equals(reminder.getReminderType())) {
                suggestions.add(new RelationshipSuggestion(
                        reminder.getPersonId(),
                        reminder.getPersonName(),
                        "MEDIUM",
                        "更新关系资料",
                        reminder.getDetail(),
                        "补充最近一次联系、近况或新的兴趣信息。",
                        "RULE"
                ));
                usedPersons.add(reminder.getPersonId());
            }
        }

        for (Person person : persons) {
            if (suggestions.size() >= MAX_SUGGESTIONS || usedPersons.contains(person.getId())) {
                continue;
            }

            List<PersonMemory> memories = personMemoryMapper.findByPersonId(userId, person.getId());
            PersonMemory hobby = memories.stream()
                    .filter(memory -> "HOBBY".equalsIgnoreCase(memory.getMemoryType()))
                    .findFirst()
                    .orElse(null);

            if (hobby != null) {
                suggestions.add(new RelationshipSuggestion(
                        person.getId(),
                        person.getName(),
                        "LOW",
                        "围绕兴趣开启交流",
                        person.getName() + "的记录中提到：" + hobby.getContent(),
                        "可以围绕这个兴趣自然发起一次聊天，避免完全没有话题。",
                        "RULE"
                ));
            }
        }

        return suggestions;
    }

    private List<RelationshipSuggestion> attachPersonIds(
            List<RelationshipSuggestion> suggestions,
            List<Person> persons) {
        Map<String, Person> personMap = new HashMap<>();
        for (Person person : persons) {
            personMap.put(person.getName(), person);
        }

        List<RelationshipSuggestion> result = new ArrayList<>();
        for (RelationshipSuggestion suggestion : suggestions) {
            Person person = personMap.get(suggestion.getPersonName());
            if (person == null) {
                continue;
            }
            suggestion.setPersonId(person.getId());
            result.add(suggestion);
        }
        return result;
    }

    private JsonNode parseJson(String answer) throws Exception {
        String text = answer == null ? "" : answer.trim()
                .replaceFirst("^```(?:json)?\\s*", "")
                .replaceFirst("\\s*```$", "")
                .trim();
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return objectMapper.readTree(text.substring(start, end + 1));
        }

        int objectStart = text.indexOf('{');
        int objectEnd = text.lastIndexOf('}');
        if (objectStart >= 0 && objectEnd > objectStart) {
            return objectMapper.readTree(text.substring(objectStart, objectEnd + 1));
        }
        throw new IllegalArgumentException("模型没有返回 JSON 数组");
    }

    private String text(JsonNode node, String fieldName) {
        return node.path(fieldName).asText("").trim();
    }

    private String normalizePriority(String value) {
        String priority = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        return switch (priority) {
            case "HIGH", "MEDIUM", "LOW" -> priority;
            default -> "MEDIUM";
        };
    }

    private String valueOrUnknown(String value) {
        return value == null || value.trim().isEmpty() ? "未记录" : value.trim();
    }

    private record PersonContext(Person person, List<PersonMemory> memories) {
    }
}
