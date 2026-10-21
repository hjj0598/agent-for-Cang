package com.example.demo.service.impl;

import com.example.demo.mapper.PersonMemoryMapper;
import com.example.demo.mapper.PersonMemoryRelationMapper;
import com.example.demo.pojo.PersonMemory;
import com.example.demo.pojo.PersonMemoryRelation;
import com.example.demo.service.PersonMemoryRelationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.openai.OpenAiChatModel;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class PersonMemoryRelationServiceImpl implements PersonMemoryRelationService {

    private static final Logger log = LoggerFactory.getLogger(PersonMemoryRelationServiceImpl.class);
    private static final int MAX_CANDIDATES = 6;
    private static final Set<String> SAVED_RELATION_TYPES = Set.of(
            "SAME_FACT",
            "DETAIL_EXPAND",
            "TEMPORAL_CHANGE",
            "CONTRADICTION"
    );

    @Autowired
    private PersonMemoryRelationMapper relationMapper;

    @Autowired
    private PersonMemoryMapper personMemoryMapper;

    @Autowired
    private OpenAiChatModel chatModel;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        relationMapper.createTableIfNotExists();
    }

    @Override
    public List<PersonMemoryRelation> analyzeAfterMemorySaved(PersonMemory savedMemory) {
        List<PersonMemoryRelation> savedRelations = new ArrayList<>();
        if (savedMemory == null || savedMemory.getId() == null
                || savedMemory.getUserId() == null || savedMemory.getPersonId() == null) {
            return savedRelations;
        }

        List<PersonMemory> candidates = personMemoryMapper.findByPersonId(savedMemory.getUserId(), savedMemory.getPersonId());
        int analyzed = 0;
        for (PersonMemory oldMemory : candidates) {
            if (oldMemory.getId() == null || oldMemory.getId().equals(savedMemory.getId())) {
                continue;
            }
            if (!sameMemoryType(oldMemory, savedMemory)) {
                continue;
            }
            if (analyzed >= MAX_CANDIDATES) {
                break;
            }

            analyzed++;
            RelationDecision decision = decideRelation(oldMemory, savedMemory);
            if (!SAVED_RELATION_TYPES.contains(decision.relationType())) {
                continue;
            }

            PersonMemoryRelation relation = new PersonMemoryRelation();
            relation.setUserId(savedMemory.getUserId());
            relation.setPersonId(savedMemory.getPersonId());
            relation.setOldMemoryId(oldMemory.getId());
            relation.setNewMemoryId(savedMemory.getId());
            relation.setRelationType(decision.relationType());
            relation.setRelationReason(limitLength(decision.reason(), 500));
            relation.setConfidence(decision.confidence());

            try {
                relationMapper.insert(relation);
                savedRelations.add(relation);
            } catch (Exception e) {
                log.warn("保存人物记忆演化关系失败 oldMemoryId={}, newMemoryId={}: {}",
                        oldMemory.getId(), savedMemory.getId(), e.getMessage());
            }
        }

        return savedRelations;
    }

    @Override
    public PersonMemoryRelation recordRelation(PersonMemory oldMemory,
                                               PersonMemory newMemory,
                                               String relationType,
                                               String reason,
                                               Double confidence) {
        if (oldMemory == null || newMemory == null
                || oldMemory.getId() == null || newMemory.getId() == null
                || newMemory.getUserId() == null || newMemory.getPersonId() == null) {
            return null;
        }

        String cleanType = relationType == null ? "" : relationType.trim().toUpperCase(Locale.ROOT);
        if (!SAVED_RELATION_TYPES.contains(cleanType)) {
            cleanType = "DETAIL_EXPAND";
        }

        PersonMemoryRelation relation = new PersonMemoryRelation();
        relation.setUserId(newMemory.getUserId());
        relation.setPersonId(newMemory.getPersonId());
        relation.setOldMemoryId(oldMemory.getId());
        relation.setNewMemoryId(newMemory.getId());
        relation.setRelationType(cleanType);
        relation.setRelationReason(limitLength(polishRelationReason(cleanType, oldMemory, newMemory, reason), 500));
        relation.setConfidence(confidence == null ? 0.8 : clamp(confidence));

        try {
            relationMapper.insert(relation);
            return relation;
        } catch (Exception e) {
            log.warn("保存指定人物记忆演化关系失败 oldMemoryId={}, newMemoryId={}: {}",
                    oldMemory.getId(), newMemory.getId(), e.getMessage());
            return null;
        }
    }

    @Override
    public List<PersonMemoryRelation> listByPerson(Long userId, Long personId, Integer limit) {
        return relationMapper.findByPersonId(userId, personId, normalizeLimit(limit));
    }

    @Override
    public List<PersonMemoryRelation> listByMemory(Long userId, Long personId, Long memoryId, Integer limit) {
        return relationMapper.findByMemoryId(userId, personId, memoryId, normalizeLimit(limit));
    }

    private RelationDecision decideRelation(PersonMemory oldMemory, PersonMemory newMemory) {
        try {
            String answer = chatModel.chat(buildRelationPrompt(oldMemory, newMemory));
            JsonNode root = parseJson(answer);
            String relationType = root.path("relationType").asText("UNRELATED").trim().toUpperCase(Locale.ROOT);
            String reason = root.path("reason").asText("").trim();
            double confidence = clamp(root.path("confidence").asDouble(0.6));

            if (!SAVED_RELATION_TYPES.contains(relationType) && !"UNRELATED".equals(relationType)) {
                relationType = "UNRELATED";
            }
            if (reason.isEmpty()) {
                reason = fallbackReason(relationType, oldMemory, newMemory);
            }
            reason = polishRelationReason(relationType, oldMemory, newMemory, reason);

            return new RelationDecision(relationType, reason, confidence);
        } catch (Exception e) {
            log.warn("AI判断人物记忆演化关系失败，使用兜底判断 oldMemoryId={}, newMemoryId={}: {}",
                    oldMemory.getId(), newMemory.getId(), e.getMessage());
            return fallbackDecision(oldMemory, newMemory);
        }
    }

    private String buildRelationPrompt(PersonMemory oldMemory, PersonMemory newMemory) {
        return """
                你是一个人物长期记忆演化关系分析器。请判断两条关于同一个人物、同一记忆类型的记忆之间是什么关系。

                只返回合法 JSON，不要 Markdown，不要额外解释：
                {
                  "relationType": "TEMPORAL_CHANGE",
                  "reason": "一句面向用户的中文说明，不要出现规则编号或原则编号",
                  "confidence": 0.82
                }

                relationType 只能是：
                - SAME_FACT：两条记忆表达的是同一事实或高度重复
                - DETAIL_EXPAND：新记忆是在补充旧记忆的条件、细节、场景或原因
                - TEMPORAL_CHANGE：新记忆体现随时间发生变化，例如以前/现在/最近/开始/不再/改为
                - CONTRADICTION：两条记忆在同一时间语境下明显互相矛盾
                - UNRELATED：两条记忆只是同类型，但主题不同，不需要建立关系

                判断要求：
                1. 不要简单按关键词判断，要看两条记忆是否描述同一个具体属性或对象。
                2. 如果新记忆包含时间变化语义，优先判断 TEMPORAL_CHANGE，而不是直接 CONTRADICTION。
                3. 如果可以同时成立，例如喜欢咖啡但不喜欢美式，优先 DETAIL_EXPAND。
                4. 如果只是同为爱好但对象不同，例如喜欢咖啡和喜欢篮球，返回 UNRELATED。
                5. confidence 是 0 到 1 的小数，表示你对关系判断的置信度。
                6. reason 必须写给普通用户看，禁止出现“规则1”“原则4”“符合原则”等内部编号。

                旧记忆：
                类型：%s
                内容：%s
                来源：%s

                新记忆：
                类型：%s
                内容：%s
                来源：%s
                """.formatted(
                valueOrUnknown(oldMemory.getMemoryType()),
                valueOrUnknown(oldMemory.getContent()),
                valueOrUnknown(oldMemory.getSource()),
                valueOrUnknown(newMemory.getMemoryType()),
                valueOrUnknown(newMemory.getContent()),
                valueOrUnknown(newMemory.getSource())
        );
    }

    private RelationDecision fallbackDecision(PersonMemory oldMemory, PersonMemory newMemory) {
        String oldContent = normalizeContent(oldMemory.getContent());
        String newContent = normalizeContent(newMemory.getContent());
        if (oldContent.isEmpty() || newContent.isEmpty()) {
            return new RelationDecision("UNRELATED", "记忆内容不足，未建立演化关系。", 0.0);
        }

        String relationType = "UNRELATED";
        double confidence = 0.45;
        if (hasTemporalSignal(newMemory.getContent()) && hasMeaningOverlap(oldContent, newContent)) {
            relationType = "TEMPORAL_CHANGE";
            confidence = 0.62;
        } else if (isOpposite(oldMemory.getContent(), newMemory.getContent()) && hasMeaningOverlap(oldContent, newContent)) {
            relationType = "CONTRADICTION";
            confidence = 0.58;
        } else if (oldContent.equals(newContent)) {
            relationType = "SAME_FACT";
            confidence = 0.7;
        } else if (oldContent.contains(newContent) || newContent.contains(oldContent)) {
            relationType = "DETAIL_EXPAND";
            confidence = 0.6;
        }

        return new RelationDecision(relationType, fallbackReason(relationType, oldMemory, newMemory), confidence);
    }

    private String fallbackReason(String relationType, PersonMemory oldMemory, PersonMemory newMemory) {
        return switch (relationType) {
            case "SAME_FACT" -> "新旧记忆表达的信息高度接近，可能是同一事实。";
            case "DETAIL_EXPAND" -> "新记忆与旧记忆主题接近，并补充了更多细节或条件。";
            case "TEMPORAL_CHANGE" -> "新记忆与旧记忆围绕同一主题，但表达出偏好或状态发生了变化。";
            case "CONTRADICTION" -> "新旧记忆围绕同一主题，但内容方向相反，需要你判断哪条更准确。";
            default -> "两条记忆主题关联较弱，未建立演化关系。";
        };
    }

    private String polishRelationReason(String relationType,
                                        PersonMemory oldMemory,
                                        PersonMemory newMemory,
                                        String reason) {
        String cleanReason = reason == null ? "" : reason.trim();
        if (!cleanReason.isEmpty() && !containsInternalRuleReference(cleanReason)) {
            return cleanReason;
        }
        return fallbackReason(relationType, oldMemory, newMemory);
    }

    private boolean containsInternalRuleReference(String reason) {
        String value = reason == null ? "" : reason.toLowerCase(Locale.ROOT);
        return value.contains("原则")
                || value.contains("规则")
                || value.contains("rule")
                || value.contains("编号")
                || value.matches(".*\\b[1-9]\\s*(and|、|,|，|和)\\s*[1-9]\\b.*");
    }

    private JsonNode parseJson(String answer) throws Exception {
        String text = answer == null ? "" : answer.trim()
                .replaceFirst("^```(?:json)?\\s*", "")
                .replaceFirst("\\s*```$", "")
                .trim();

        int objectStart = text.indexOf('{');
        int objectEnd = text.lastIndexOf('}');
        if (objectStart >= 0 && objectEnd > objectStart) {
            return objectMapper.readTree(text.substring(objectStart, objectEnd + 1));
        }

        return objectMapper.readTree(text);
    }

    private boolean sameMemoryType(PersonMemory left, PersonMemory right) {
        return normalizeType(left.getMemoryType()).equals(normalizeType(right.getMemoryType()));
    }

    private String normalizeType(String type) {
        return type == null || type.trim().isEmpty() ? "NOTE" : type.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeContent(String content) {
        if (content == null) {
            return "";
        }
        return content.toLowerCase(Locale.ROOT)
                .replaceAll("[\\s，。！？、,.!?;；:：\"'“”‘’()（）\\[\\]【】]", "")
                .replace("以前", "")
                .replace("现在", "")
                .replace("最近", "")
                .replace("开始", "")
                .replace("不再", "")
                .replace("改成", "")
                .replace("变得", "")
                .replace("不喜欢", "")
                .replace("喜欢", "")
                .replace("讨厌", "")
                .replace("常喝", "")
                .replace("不喝", "")
                .replace("爱", "")
                .replace("dislike", "")
                .replace("like", "");
    }

    private boolean hasTemporalSignal(String content) {
        String value = content == null ? "" : content;
        return value.contains("以前")
                || value.contains("现在")
                || value.contains("最近")
                || value.contains("开始")
                || value.contains("喜欢上")
                || value.contains("不再")
                || value.contains("改成")
                || value.contains("变得")
                || value.matches(".*(不喜欢|不喝|不吃|不太喝|不怎么喝|少喝|戒|停止).{0,8}了.*")
                || value.toLowerCase(Locale.ROOT).contains("now")
                || value.toLowerCase(Locale.ROOT).contains("recently")
                || value.toLowerCase(Locale.ROOT).contains("used to");
    }

    private boolean isOpposite(String oldContent, String newContent) {
        return isPositive(oldContent) && isNegative(newContent)
                || isNegative(oldContent) && isPositive(newContent);
    }

    private boolean isPositive(String content) {
        String value = content == null ? "" : content;
        return value.contains("喜欢")
                || value.contains("爱")
                || value.contains("常喝")
                || value.toLowerCase(Locale.ROOT).contains("like");
    }

    private boolean isNegative(String content) {
        String value = content == null ? "" : content;
        return value.contains("不喜欢")
                || value.contains("讨厌")
                || value.contains("不喝")
                || value.contains("不吃")
                || value.toLowerCase(Locale.ROOT).contains("dislike")
                || value.toLowerCase(Locale.ROOT).contains("do not like")
                || value.toLowerCase(Locale.ROOT).contains("does not like");
    }

    private boolean hasMeaningOverlap(String left, String right) {
        return !left.isEmpty()
                && !right.isEmpty()
                && (left.contains(right) || right.contains(left) || longestCommonLength(left, right) >= 2);
    }

    private int longestCommonLength(String left, String right) {
        int max = 0;
        for (int i = 0; i < left.length(); i++) {
            for (int j = 0; j < right.length(); j++) {
                int length = 0;
                while (i + length < left.length()
                        && j + length < right.length()
                        && left.charAt(i + length) == right.charAt(j + length)) {
                    length++;
                }
                max = Math.max(max, length);
            }
        }
        return max;
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return 50;
        }
        return Math.min(limit, 200);
    }

    private String limitLength(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String valueOrUnknown(String value) {
        return value == null || value.trim().isEmpty() ? "未记录" : value.trim();
    }

    private record RelationDecision(String relationType, String reason, double confidence) {
    }
}
