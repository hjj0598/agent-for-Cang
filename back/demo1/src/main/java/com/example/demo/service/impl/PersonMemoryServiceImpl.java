package com.example.demo.service.impl;

import com.example.demo.dto.MemoryConflictResult;
import com.example.demo.mapper.PersonMemoryMapper;
import com.example.demo.mapper.PersonMapper;
import com.example.demo.pojo.Person;
import com.example.demo.pojo.PersonMemory;
import com.example.demo.pojo.PersonMemoryRelation;
import com.example.demo.service.PersonMemoryEmbeddingService;
import com.example.demo.service.PersonMemoryRelationService;
import com.example.demo.service.PersonMemoryService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class PersonMemoryServiceImpl implements PersonMemoryService {

    private static final Logger log = LoggerFactory.getLogger(PersonMemoryServiceImpl.class);

    @Autowired
    private PersonMemoryMapper personMemoryMapper;

    @Autowired
    private PersonMapper personMapper;

    @Autowired
    private OpenAiChatModel chatModel;

    @Autowired
    private PersonMemoryEmbeddingService personMemoryEmbeddingService;

    @Autowired
    private PersonMemoryRelationService personMemoryRelationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<PersonMemory> list(Long userId, Long personId) {
        if (personMapper.findByIdAndUserId(personId, userId) == null) {
            throw new RuntimeException("人物不存在");
        }

        return enrichSources(personMemoryMapper.findByPersonId(userId, personId));
    }

    @Override
    public void add(PersonMemory memory) {
        if (personMapper.findByIdAndUserId(memory.getPersonId(), memory.getUserId()) == null) {
            throw new RuntimeException("人物不存在");
        }

        normalizeMemory(memory);
        personMemoryMapper.insert(memory);
        indexMemorySafely(memory);
        analyzeMemoryRelationsSafely(memory);
    }

    @Override
    public MemoryConflictResult addWithConflictCheck(PersonMemory memory, String conflictAction, Long conflictMemoryId) {
        if (personMapper.findByIdAndUserId(memory.getPersonId(), memory.getUserId()) == null) {
            throw new RuntimeException("人物不存在");
        }

        normalizeMemory(memory);

        ConflictDecision conflictDecision = findConflict(memory);
        if (conflictDecision != null && !isConflictAction(conflictAction)) {
            return buildNeedConfirmResult(conflictDecision);
        }

        PersonMemory relationOldMemory = resolveRelationOldMemory(memory, conflictDecision, conflictMemoryId);
        String relationType = conflictDecision == null ? "" : conflictDecision.relationType();
        String relationReason = conflictDecision == null ? "" : conflictDecision.reason();

        if ("REPLACE_OLD".equalsIgnoreCase(conflictAction)) {
            Long targetId = relationOldMemory == null ? null : relationOldMemory.getId();
            if (targetId == null) {
                throw new RuntimeException("缺少要替换的旧记忆ID");
            }
            PersonMemory target = personMemoryMapper.findByIdAndUserId(targetId, memory.getUserId());
            if (target == null || !target.getPersonId().equals(memory.getPersonId())) {
                throw new RuntimeException("要替换的旧记忆不存在");
            }
            personMemoryMapper.deleteById(target.getId(), target.getPersonId(), memory.getUserId());
            relationOldMemory = null;
            relationType = "";
            relationReason = "";
        }

        personMemoryMapper.insert(memory);
        indexMemorySafely(memory);
        MemoryConflictResult result = MemoryConflictResult.saved(memory);
        result.setMemoryRelations(buildRelationsAfterSave(memory, relationOldMemory, relationType, relationReason));
        return result;
    }

    private void normalizeMemory(PersonMemory memory) {
        if (memory.getMemoryType() == null || memory.getMemoryType().trim().isEmpty()) {
            memory.setMemoryType("NOTE");
        }

        if (memory.getSource() == null || memory.getSource().trim().isEmpty()) {
            memory.setSource("手动记录");
        }
    }

    @Override
    public void delete(Long userId, Long personId, Long memoryId) {
        personMemoryMapper.deleteById(memoryId, personId, userId);
    }

    @Override
    public List<PersonMemory> findByPersonName(String personName) {
        return enrichSources(personMemoryMapper.findByPersonName(personName));
    }


    @Override
    public void addByPersonName(String personName, String memoryType, String content, String source) {
        if (personName == null || personName.trim().isEmpty()) {
            throw new RuntimeException("人物姓名不能为空");
        }

        if (content == null || content.trim().isEmpty()) {
            throw new RuntimeException("记忆内容不能为空");
        }

        Person person = personMapper.findByName(personName);

        if (person == null) {
            throw new RuntimeException("人物不存在，请先在人物管理中创建：" + personName);
        }

        PersonMemory memory = new PersonMemory();
        memory.setUserId(person.getUserId());
        memory.setPersonId(person.getId());
        memory.setMemoryType(memoryType == null || memoryType.trim().isEmpty() ? "NOTE" : memoryType);
        memory.setContent(content);
        memory.setSource(source == null || source.trim().isEmpty() ? "AI聊天记录" : source);

        personMemoryMapper.insert(memory);
        indexMemorySafely(memory);
        analyzeMemoryRelationsSafely(memory);
    }



    @Override
    public List<PersonMemory> findByUserIdAndPersonName(Long userId, String personName) {
        return enrichSources(personMemoryMapper.findByUserIdAndPersonName(userId, personName));
    }

    @Override
    public List<PersonMemory> searchByUserIdAndPersonName(Long userId, String personName) {
        if (personName == null || personName.trim().isEmpty()) {
            throw new RuntimeException("搜索人物姓名不能为空");
        }

        return enrichSources(personMemoryMapper.searchByUserIdAndPersonName(userId, personName.trim()));
    }

    @Override
    public MemoryConflictResult addByUserIdAndPersonName(Long userId,
                                                         String personName,
                                                         String memoryType,
                                                         String content,
                                                         String source) {
        if (personName == null || personName.trim().isEmpty()) {
            throw new RuntimeException("人物姓名不能为空");
        }

        if (content == null || content.trim().isEmpty()) {
            throw new RuntimeException("记忆内容不能为空");
        }

        Person person = personMapper.findByUserIdAndName(userId, personName);

        if (person == null) {
            person = new Person();
            person.setUserId(userId);
            person.setName(personName);
            person.setRelation("未设置");
            person.setDescription("AI聊天中自动创建的人物");

            personMapper.insert(person);

            person = personMapper.findByUserIdAndName(userId, personName);
        }

        PersonMemory memory = new PersonMemory();
        memory.setUserId(userId);
        memory.setPersonId(person.getId());
        memory.setMemoryType(memoryType == null || memoryType.trim().isEmpty() ? "NOTE" : memoryType);
        memory.setContent(content);
        memory.setSource(source == null || source.trim().isEmpty() ? "AI聊天记录" : source);

        ConflictDecision conflictDecision = findConflict(memory);
        if (conflictDecision != null) {
            return buildNeedConfirmResult(conflictDecision);
        }

        personMemoryMapper.insert(memory);
        indexMemorySafely(memory);
        MemoryConflictResult result = MemoryConflictResult.saved(memory);
        result.setMemoryRelations(analyzeMemoryRelationsSafely(memory));
        return result;
    }

    private void indexMemorySafely(PersonMemory memory) {
        try {
            personMemoryEmbeddingService.index(memory);
        } catch (Exception e) {
            log.warn("人物记忆向量索引失败 memoryId={}: {}", memory == null ? null : memory.getId(), e.getMessage());
        }
    }

    private List<PersonMemoryRelation> analyzeMemoryRelationsSafely(PersonMemory memory) {
        try {
            return personMemoryRelationService.analyzeAfterMemorySaved(memory);
        } catch (Exception e) {
            log.warn("人物记忆演化关系分析失败 memoryId={}: {}", memory == null ? null : memory.getId(), e.getMessage());
            return new ArrayList<>();
        }
    }

    private MemoryConflictResult buildNeedConfirmResult(ConflictDecision decision) {
        if ("CHANGE".equals(decision.status())) {
            return MemoryConflictResult.change(decision.memory(), decision.reason());
        }

        return MemoryConflictResult.conflict(decision.memory(), decision.reason());
    }

    private PersonMemory resolveRelationOldMemory(PersonMemory memory,
                                                  ConflictDecision conflictDecision,
                                                  Long conflictMemoryId) {
        if (conflictMemoryId != null) {
            PersonMemory target = personMemoryMapper.findByIdAndUserId(conflictMemoryId, memory.getUserId());
            if (target == null || !target.getPersonId().equals(memory.getPersonId())) {
                throw new RuntimeException("要处理的旧记忆不存在");
            }
            enrichSource(target);
            return target;
        }

        return conflictDecision == null ? null : conflictDecision.memory();
    }

    private List<PersonMemoryRelation> buildRelationsAfterSave(PersonMemory memory,
                                                               PersonMemory relationOldMemory,
                                                               String relationType,
                                                               String relationReason) {
        if (relationOldMemory != null && relationType != null && !relationType.trim().isEmpty()) {
            PersonMemoryRelation relation = personMemoryRelationService.recordRelation(
                    relationOldMemory,
                    memory,
                    relationType,
                    relationReason,
                    "TEMPORAL_CHANGE".equals(relationType) ? 0.85 : 0.75
            );

            List<PersonMemoryRelation> relations = new ArrayList<>();
            if (relation != null) {
                relations.add(relation);
            }
            return relations;
        }

        return analyzeMemoryRelationsSafely(memory);
    }

    private ConflictDecision findConflict(PersonMemory incoming) {
        List<PersonMemory> memories = personMemoryMapper.findByPersonId(incoming.getUserId(), incoming.getPersonId());
        for (PersonMemory existing : memories) {
            if (existing.getId() != null && existing.getId().equals(incoming.getId())) {
                continue;
            }
            if (isRuleConflict(existing, incoming)) {
                enrichSource(existing);
                ConflictDecision aiDecision = verifyConflictWithAi(existing, incoming);
                if (aiDecision.needsConfirmation()) {
                    return aiDecision;
                }
            }
        }
        return null;
    }

    private boolean isRuleConflict(PersonMemory existing, PersonMemory incoming) {
        String oldType = normalizeType(existing.getMemoryType());
        String newType = normalizeType(incoming.getMemoryType());
        String oldContent = normalizeContent(existing.getContent());
        String newContent = normalizeContent(incoming.getContent());

        if (oldContent.isEmpty() || newContent.isEmpty()) {
            return false;
        }

        boolean oppositeType = ("HOBBY".equals(oldType) && "DISLIKE".equals(newType))
                || ("DISLIKE".equals(oldType) && "HOBBY".equals(newType));
        boolean oppositeMeaning = isPositive(existing.getContent()) && isNegative(incoming.getContent())
                || isNegative(existing.getContent()) && isPositive(incoming.getContent());
        boolean meaningOverlap = oldContent.contains(newContent)
                || newContent.contains(oldContent)
                || longestCommonLength(oldContent, newContent) >= 2;

        if (hasTemporalSignal(incoming.getContent()) && meaningOverlap) {
            return true;
        }

        if (!oppositeType && !oppositeMeaning) {
            return false;
        }

        return meaningOverlap;
    }

    private String normalizeType(String type) {
        return type == null ? "NOTE" : type.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isMutableMemoryType(PersonMemory oldMemory, PersonMemory newMemory) {
        String oldType = normalizeType(oldMemory.getMemoryType());
        String newType = normalizeType(newMemory.getMemoryType());
        return isMutableType(oldType) || isMutableType(newType);
    }

    private boolean isMutableType(String type) {
        return "HOBBY".equals(type)
                || "DISLIKE".equals(type)
                || "TRAIT".equals(type)
                || "NOTE".equals(type);
    }

    private boolean isConflictAction(String conflictAction) {
        return "KEEP_BOTH".equalsIgnoreCase(conflictAction)
                || "REPLACE_OLD".equalsIgnoreCase(conflictAction);
    }

    private String buildConflictReason(PersonMemory oldMemory, PersonMemory newMemory) {
        return "新记忆与已有记忆在偏好方向上可能相反：旧记录「"
                + oldMemory.getContent()
                + "」，新记录「"
                + newMemory.getContent()
                + "」。";
    }

    private String buildChangeReason(PersonMemory oldMemory, PersonMemory newMemory) {
        return "新记忆可能表示人物信息发生变化：旧记录「"
                + oldMemory.getContent()
                + "」，新记录「"
                + newMemory.getContent()
                + "」。";
    }

    private ConflictDecision verifyConflictWithAi(PersonMemory oldMemory, PersonMemory newMemory) {
        String fallbackReason = buildConflictReason(oldMemory, newMemory);
        String fallbackChangeReason = buildChangeReason(oldMemory, newMemory);

        try {
            String answer = chatModel.chat(buildConflictPrompt(oldMemory, newMemory));
            JsonNode root = parseJson(answer);
            String status = root.path("status").asText("CONFLICT").trim().toUpperCase(Locale.ROOT);
            String reason = root.path("reason").asText("").trim();

            if ("NOT_CONFLICT".equals(status)) {
                return new ConflictDecision(oldMemory, false, "NONE", "", reason.isEmpty() ? "不是冲突或变化。" : reason);
            }

            if ("CHANGE".equals(status)
                    || "TEMPORAL_CHANGE".equals(status)
                    || ("CONFLICT".equals(status) && isMutableMemoryType(oldMemory, newMemory))
                    || ("POSSIBLE".equals(status) && hasTemporalSignal(newMemory.getContent()))) {
                reason = polishDecisionReason("CHANGE", oldMemory, newMemory, reason);
                return new ConflictDecision(oldMemory, true, "CHANGE", "TEMPORAL_CHANGE", reason);
            }

            reason = polishDecisionReason("CONFLICT", oldMemory, newMemory, reason);
            return new ConflictDecision(oldMemory, true, "CONFLICT", "CONTRADICTION", reason);
        } catch (Exception e) {
            log.warn("AI辅助判断记忆冲突失败，使用规则兜底: {}", e.getMessage());
            if (hasTemporalSignal(newMemory.getContent()) || isMutableMemoryType(oldMemory, newMemory)) {
                return new ConflictDecision(oldMemory, true, "CHANGE", "TEMPORAL_CHANGE", fallbackChangeReason);
            }
            return new ConflictDecision(oldMemory, true, "CONFLICT", "CONTRADICTION", fallbackReason);
        }
    }

    private String buildConflictPrompt(PersonMemory oldMemory, PersonMemory newMemory) {
        return """
                你是一个长期人物记忆冲突审核器。请判断两条关于同一个人物的记忆是否存在事实冲突。

                只返回合法 JSON，不要 Markdown，不要解释额外文字：
                {
                  "status": "CHANGE",
                  "reason": "面向用户的一句话说明，不要出现规则编号或原则编号",
                  "suggestion": "KEEP_BOTH"
                }

                status 只能是：
                - CHANGE：新记忆可能表示人物偏好、状态、习惯或性格等可变化信息发生变化
                - CONFLICT：两条记忆在稳定事实上明显互相矛盾，例如生日、日期、客观身份信息
                - NOT_CONFLICT：不是冲突，通常是细分偏好、条件不同或表达可同时成立

                内部判断标准：
                1. 如果新记忆带有时间变化语义，返回 CHANGE。
                   例如：以前喜欢咖啡，现在不喝咖啡；最近不吃辣；开始喝无糖咖啡。
                2. 如果两条记忆指向同一大类下的不同子类，通常返回 NOT_CONFLICT。
                   例如：喜欢拿铁 vs 不喜欢美式；喜欢篮球 vs 不喜欢足球。
                3. 如果一条是大类，另一条是子类，且方向相反，返回 CHANGE，因为可能是偏好细化或变化。
                   例如：喜欢水果 vs 不喜欢青苹果；喜欢狗 vs 不喜欢大型犬。
                4. 对爱好、偏好、习惯、性格等可变化信息，如果两条记忆指向同一个对象但方向相反，返回 CHANGE。
                   例如：喜欢咖啡 vs 不喜欢咖啡；爱跑步 vs 讨厌跑步。
                5. 如果存在条件、场景、原因限制，优先判断是否可以同时成立。
                   例如：不喜欢咖啡店环境，但喜欢喝咖啡，应返回 NOT_CONFLICT。
                6. 对生日、重要日期、客观身份等稳定事实，如果两条记录明显不一致，返回 CONFLICT。
                7. 不要判断哪条一定正确，只判断是否需要用户确认。
                8. reason 必须写给普通用户看，禁止出现“规则1”“原则4”“符合原则”等内部编号。

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

    private String valueOrUnknown(String value) {
        return value == null || value.trim().isEmpty() ? "未记录" : value.trim();
    }

    private String polishDecisionReason(String status,
                                        PersonMemory oldMemory,
                                        PersonMemory newMemory,
                                        String reason) {
        String cleanReason = reason == null ? "" : reason.trim();
        if (!cleanReason.isEmpty() && !containsInternalRuleReference(cleanReason)) {
            return cleanReason;
        }

        if ("CHANGE".equals(status)) {
            return buildChangeReason(oldMemory, newMemory);
        }
        return buildConflictReason(oldMemory, newMemory);
    }

    private boolean containsInternalRuleReference(String reason) {
        String value = reason == null ? "" : reason.toLowerCase(Locale.ROOT);
        return value.contains("原则")
                || value.contains("规则")
                || value.contains("rule")
                || value.contains("编号")
                || value.matches(".*\\b[1-9]\\s*(and|、|,|，|和)\\s*[1-9]\\b.*");
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
                || value.contains("不太喝")
                || value.contains("不怎么喝")
                || value.contains("少喝")
                || value.contains("不吃")
                || value.contains("不再")
                || value.contains("戒")
                || value.contains("停止")
                || value.toLowerCase(Locale.ROOT).contains("dislike")
                || value.toLowerCase(Locale.ROOT).contains("do not like")
                || value.toLowerCase(Locale.ROOT).contains("does not like");
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
                || value.contains("后来")
                || value.contains("目前")
                || value.matches(".*(不喜欢|不喝|不吃|不太喝|不怎么喝|少喝|戒|停止).{0,8}了.*")
                || value.toLowerCase(Locale.ROOT).contains("now")
                || value.toLowerCase(Locale.ROOT).contains("recently")
                || value.toLowerCase(Locale.ROOT).contains("used to");
    }

    private String normalizeContent(String content) {
        if (content == null) {
            return "";
        }
        return content.toLowerCase(Locale.ROOT)
                .replaceAll("[\\s，。！？、,.!?;；:：\"'“”‘’()（）\\[\\]【】]", "")
                .replace("不喜欢", "")
                .replace("喜欢", "")
                .replace("讨厌", "")
                .replace("不太喝", "")
                .replace("不怎么喝", "")
                .replace("少喝", "")
                .replace("不喝", "")
                .replace("常喝", "")
                .replace("不吃", "")
                .replace("不再", "")
                .replace("戒", "")
                .replace("停止", "")
                .replace("爱", "")
                .replace("dislike", "")
                .replace("doesnotlike", "")
                .replace("donotlike", "")
                .replace("like", "");
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

    /**
     * source 是历史表中已有的字符串字段。
     * 这里把它拆成前端容易展示的来源类型和引用，不改变数据库结构。
     */
    private List<PersonMemory> enrichSources(List<PersonMemory> memories) {
        if (memories == null || memories.isEmpty()) {
            return memories == null ? new ArrayList<>() : memories;
        }

        memories.forEach(this::enrichSource);
        return memories;
    }

    private void enrichSource(PersonMemory memory) {
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
        } else if (cleanSource.contains("AI聊天") || cleanSource.contains("AI chat")) {
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

    private record ConflictDecision(PersonMemory memory,
                                    boolean needsConfirmation,
                                    String status,
                                    String relationType,
                                    String reason) {
    }
}
