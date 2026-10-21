package com.example.demo.service.impl;

import com.example.demo.dto.CangjieMemoryGuardResult;
import com.example.demo.dto.MemoryCandidateBatchCreateRequest;
import com.example.demo.dto.MemoryCandidateConfirmRequest;
import com.example.demo.dto.MemoryCandidateConfirmResult;
import com.example.demo.dto.MemoryCandidateCreateRequest;
import com.example.demo.dto.MemoryCandidateRejectRequest;
import com.example.demo.dto.MemoryCandidateUpdateRequest;
import com.example.demo.dto.MemoryConflictResult;
import com.example.demo.mapper.MemoryCandidateMapper;
import com.example.demo.mapper.PersonMapper;
import com.example.demo.pojo.MemoryCandidateRecord;
import com.example.demo.pojo.Person;
import com.example.demo.pojo.PersonMemory;
import com.example.demo.service.CangjieMemoryGuardService;
import com.example.demo.service.MemoryCandidateService;
import com.example.demo.service.PersonMemoryService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class MemoryCandidateServiceImpl implements MemoryCandidateService {

    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 200;

    @Autowired
    private MemoryCandidateMapper memoryCandidateMapper;

    @Autowired
    private PersonMapper personMapper;

    @Autowired
    private PersonMemoryService personMemoryService;

    @Autowired
    private CangjieMemoryGuardService cangjieMemoryGuardService;

    @PostConstruct
    public void initTable() {
        memoryCandidateMapper.createTableIfNotExists();
    }

    @Override
    public List<MemoryCandidateRecord> list(Long userId, String status, Long personId, Integer limit) {
        return memoryCandidateMapper.findByUserId(
                userId,
                normalizeStatusFilter(status),
                personId,
                normalizeLimit(limit)
        );
    }

    @Override
    public MemoryCandidateRecord getById(Long userId, Long id) {
        MemoryCandidateRecord candidate = memoryCandidateMapper.findByIdAndUserId(id, userId);
        if (candidate == null) {
            throw new RuntimeException("候选记忆不存在");
        }
        return candidate;
    }

    @Override
    public MemoryCandidateRecord create(Long userId, MemoryCandidateCreateRequest request) {
        MemoryCandidateRecord candidate = buildCandidate(userId, request);
        memoryCandidateMapper.insert(candidate);
        return getById(userId, candidate.getId());
    }

    @Override
    public List<MemoryCandidateRecord> createBatch(Long userId, MemoryCandidateBatchCreateRequest request) {
        List<MemoryCandidateRecord> rows = new ArrayList<>();
        for (MemoryCandidateCreateRequest item : request.getCandidates()) {
            MemoryCandidateCreateRequest merged = mergeBatchDefaults(item, request);
            rows.add(create(userId, merged));
        }
        return rows;
    }

    @Override
    public MemoryCandidateRecord update(Long userId, Long id, MemoryCandidateUpdateRequest request) {
        MemoryCandidateRecord existing = requirePending(userId, id);
        existing.setPersonId(request.getPersonId());
        existing.setPersonName(clean(request.getPersonName()));
        existing.setMemoryType(normalizeMemoryType(request.getMemoryType()));
        existing.setContent(cleanRequired(request.getContent(), "候选记忆内容不能为空"));
        existing.setSourceType(normalizeSourceType(request.getSourceType()));
        existing.setSourceReference(clean(request.getSourceReference()));
        existing.setSource(buildSource(existing.getSourceType(), existing.getSourceReference()));
        existing.setConfidence(normalizeConfidence(request.getConfidence()));
        existing.setFactType(normalizeFactType(request.getFactType()));
        existing.setTrustLevel(normalizeTrustLevel(request.getTrustLevel(), existing.getConfidence(), existing.getFactType()));
        existing.setRawEvidence(clean(request.getRawEvidence()));

        int updated = memoryCandidateMapper.updatePending(existing);
        if (updated == 0) {
            throw new RuntimeException("候选记忆状态已变化，无法修改");
        }
        return getById(userId, id);
    }

    @Transactional
    @Override
    public MemoryCandidateConfirmResult confirm(Long userId, Long id, MemoryCandidateConfirmRequest request) {
        if (request == null) {
            request = new MemoryCandidateConfirmRequest();
        }

        MemoryCandidateRecord candidate = requirePending(userId, id);
        Person person = resolvePerson(userId, candidate, request);

        PersonMemory memory = new PersonMemory();
        memory.setUserId(userId);
        memory.setPersonId(person.getId());
        memory.setMemoryType(firstNotBlank(request.getMemoryType(), candidate.getMemoryType(), "NOTE"));
        memory.setContent(firstNotBlank(request.getContent(), candidate.getContent(), null));
        memory.setSource(firstNotBlank(request.getSource(), candidate.getSource(), buildSource(candidate.getSourceType(), candidate.getSourceReference())));

        if (memory.getContent() == null || memory.getContent().trim().isEmpty()) {
            throw new RuntimeException("确认记忆内容不能为空");
        }

        memory.setSource(truncate(memory.getSource(), 100));

        MemoryConflictResult memoryResult = personMemoryService.addWithConflictCheck(
                memory,
                request.getConflictAction(),
                request.getConflictMemoryId()
        );

        if ("CONFLICT".equals(memoryResult.getStatus()) || "CHANGE".equals(memoryResult.getStatus())) {
            return MemoryCandidateConfirmResult.of(memoryResult, candidate);
        }

        Long savedMemoryId = memoryResult.getSavedMemory() == null ? null : memoryResult.getSavedMemory().getId();
        memoryCandidateMapper.markConfirmed(id, userId, savedMemoryId);
        return MemoryCandidateConfirmResult.of(memoryResult, getById(userId, id));
    }

    @Override
    public MemoryCandidateRecord reject(Long userId, Long id, MemoryCandidateRejectRequest request) {
        requirePending(userId, id);
        String reason = request == null ? "" : clean(request.getReason());
        int updated = memoryCandidateMapper.markRejected(id, userId, truncate(reason, 255));
        if (updated == 0) {
            throw new RuntimeException("候选记忆状态已变化，无法拒绝");
        }
        return getById(userId, id);
    }

    @Override
    public void delete(Long userId, Long id) {
        memoryCandidateMapper.deleteByIdAndUserId(id, userId);
    }

    private MemoryCandidateRecord buildCandidate(Long userId, MemoryCandidateCreateRequest request) {
        String content = cleanRequired(request.getContent(), "候选记忆内容不能为空");
        CangjieMemoryGuardResult guardResult = cangjieMemoryGuardService.analyze(content);

        MemoryCandidateRecord candidate = new MemoryCandidateRecord();
        candidate.setUserId(userId);
        candidate.setPersonId(request.getPersonId());
        candidate.setPersonName(clean(request.getPersonName()));
        candidate.setMemoryType(normalizeMemoryType(firstNotBlank(
                request.getMemoryType(),
                guardResult == null ? null : guardResult.getMemoryType(),
                null
        )));
        candidate.setContent(content);
        candidate.setSourceType(normalizeSourceType(request.getSourceType()));
        candidate.setSourceReference(firstNotBlank(
                request.getSourceReference(),
                buildGuardSourceReference(guardResult),
                null
        ));
        candidate.setSource(buildSource(candidate.getSourceType(), candidate.getSourceReference()));
        candidate.setConfidence(normalizeConfidence(firstNotNull(
                request.getConfidence(),
                guardResult == null ? null : guardResult.getConfidence()
        )));
        candidate.setFactType(normalizeFactType(firstNotBlank(
                request.getFactType(),
                guardResult == null ? null : guardResult.getFactType(),
                null
        )));
        candidate.setTrustLevel(normalizeTrustLevel(
                firstNotBlank(request.getTrustLevel(), guardResult == null ? null : guardResult.getTrustLevel(), null),
                candidate.getConfidence(),
                candidate.getFactType()
        ));
        candidate.setStatus("PENDING");
        candidate.setRawEvidence(buildRawEvidence(clean(request.getRawEvidence()), guardResult));
        return candidate;
    }

    private Double firstNotNull(Double first, Double second) {
        return first == null ? second : first;
    }

    private String buildGuardSourceReference(CangjieMemoryGuardResult guardResult) {
        if (!isUsefulGuardResult(guardResult) || clean(guardResult.getReason()) == null) {
            return null;
        }
        return truncate("仓颉规则：" + clean(guardResult.getReason()), 255);
    }

    private String buildRawEvidence(String rawEvidence, CangjieMemoryGuardResult guardResult) {
        if (rawEvidence != null || !isUsefulGuardResult(guardResult)) {
            return rawEvidence;
        }

        return "{"
                + "\"cangjieRule\":{"
                + "\"memoryType\":\"" + escapeJson(guardResult.getMemoryType()) + "\","
                + "\"trustLevel\":\"" + escapeJson(guardResult.getTrustLevel()) + "\","
                + "\"factType\":\"" + escapeJson(guardResult.getFactType()) + "\","
                + "\"riskLevel\":\"" + escapeJson(guardResult.getRiskLevel()) + "\","
                + "\"needsReview\":" + booleanJson(guardResult.getNeedsReview()) + ","
                + "\"hasTemporalChange\":" + booleanJson(guardResult.getHasTemporalChange()) + ","
                + "\"hasConflictRisk\":" + booleanJson(guardResult.getHasConflictRisk()) + ","
                + "\"reason\":\"" + escapeJson(guardResult.getReason()) + "\""
                + "}"
                + "}";
    }

    private boolean isUsefulGuardResult(CangjieMemoryGuardResult guardResult) {
        return guardResult != null
                && (clean(guardResult.getMemoryType()) != null
                || clean(guardResult.getTrustLevel()) != null
                || clean(guardResult.getFactType()) != null
                || clean(guardResult.getRiskLevel()) != null
                || clean(guardResult.getReason()) != null);
    }

    private String booleanJson(Boolean value) {
        return Boolean.TRUE.equals(value) ? "true" : "false";
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }
    private MemoryCandidateCreateRequest mergeBatchDefaults(MemoryCandidateCreateRequest item,
                                                            MemoryCandidateBatchCreateRequest batch) {
        MemoryCandidateCreateRequest merged = new MemoryCandidateCreateRequest();
        merged.setPersonId(item.getPersonId() == null ? batch.getPersonId() : item.getPersonId());
        merged.setPersonName(firstNotBlank(item.getPersonName(), batch.getPersonName(), null));
        merged.setMemoryType(item.getMemoryType());
        merged.setContent(item.getContent());
        merged.setSourceType(firstNotBlank(item.getSourceType(), batch.getSourceType(), null));
        merged.setSourceReference(firstNotBlank(item.getSourceReference(), batch.getSourceReference(), null));
        merged.setConfidence(item.getConfidence());
        merged.setFactType(item.getFactType());
        merged.setTrustLevel(item.getTrustLevel());
        merged.setRawEvidence(firstNotBlank(item.getRawEvidence(), batch.getRawEvidence(), null));
        return merged;
    }

    private MemoryCandidateRecord requirePending(Long userId, Long id) {
        MemoryCandidateRecord candidate = getById(userId, id);
        if (!"PENDING".equalsIgnoreCase(candidate.getStatus())) {
            throw new RuntimeException("候选记忆不是待确认状态");
        }
        return candidate;
    }

    private Person resolvePerson(Long userId,
                                 MemoryCandidateRecord candidate,
                                 MemoryCandidateConfirmRequest request) {
        Long personId = request.getPersonId() == null ? candidate.getPersonId() : request.getPersonId();
        if (personId != null) {
            Person person = personMapper.findByIdAndUserId(personId, userId);
            if (person == null) {
                throw new RuntimeException("人物不存在");
            }
            return person;
        }

        String personName = firstNotBlank(request.getPersonName(), candidate.getPersonName(), null);
        if (personName == null) {
            throw new RuntimeException("请先选择人物或填写人物姓名");
        }

        Person person = personMapper.findByUserIdAndName(userId, personName);
        if (person != null) {
            return person;
        }

        person = new Person();
        person.setUserId(userId);
        person.setName(personName);
        person.setRelation("未设置");
        person.setDescription("候选记忆确认时自动创建的人物");
        personMapper.insert(person);
        return personMapper.findByUserIdAndName(userId, personName);
    }

    private String normalizeStatusFilter(String status) {
        String value = clean(status);
        return value == null ? null : value.toUpperCase(Locale.ROOT);
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private String normalizeMemoryType(String value) {
        String type = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        return switch (type) {
            case "HOBBY", "QUOTE", "TRAIT", "DISLIKE", "DATE", "NOTE" -> type;
            case "爱好", "兴趣", "偏好" -> "HOBBY";
            case "语录", "说过的话", "原话" -> "QUOTE";
            case "性格", "性格特点", "特点" -> "TRAIT";
            case "不喜欢", "讨厌" -> "DISLIKE";
            case "日期", "重要日期", "生日", "纪念日" -> "DATE";
            default -> "NOTE";
        };
    }

    private String normalizeSourceType(String value) {
        String sourceType = clean(value);
        return sourceType == null ? "手动记录" : sourceType;
    }

    private Double normalizeConfidence(Double confidence) {
        if (confidence == null) {
            return 0.7D;
        }
        return Math.max(0D, Math.min(1D, confidence));
    }

    private String normalizeFactType(String value) {
        String factType = clean(value);
        if (factType == null) {
            return "AI_EXTRACTED_FACT";
        }
        return factType.toUpperCase(Locale.ROOT);
    }

    private String normalizeTrustLevel(String value, Double confidence, String factType) {
        String trustLevel = clean(value);
        if (trustLevel != null) {
            return trustLevel.toUpperCase(Locale.ROOT);
        }

        if ("AI_INFERENCE".equalsIgnoreCase(factType) || "USER_OPINION".equalsIgnoreCase(factType)) {
            return "LOW";
        }

        double score = confidence == null ? 0.7D : confidence;
        if (score >= 0.85D) {
            return "HIGH";
        }
        if (score < 0.5D) {
            return "LOW";
        }
        return "MEDIUM";
    }

    private String buildSource(String sourceType, String sourceReference) {
        String type = firstNotBlank(sourceType, "手动记录", null);
        String reference = clean(sourceReference);
        if (reference == null) {
            return type;
        }
        return type + "：" + reference;
    }

    private String cleanRequired(String value, String message) {
        String cleanValue = clean(value);
        if (cleanValue == null) {
            throw new RuntimeException(message);
        }
        return cleanValue;
    }

    private String firstNotBlank(String first, String second, String fallback) {
        String cleanFirst = clean(first);
        if (cleanFirst != null) {
            return cleanFirst;
        }

        String cleanSecond = clean(second);
        if (cleanSecond != null) {
            return cleanSecond;
        }

        return fallback;
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String cleanValue = value.trim();
        return cleanValue.isEmpty() ? null : cleanValue;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}


