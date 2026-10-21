package com.example.demo.service.impl;

import com.example.demo.dto.RelationshipSuggestionActionRequest;
import com.example.demo.dto.RelationshipSuggestionActionUpdateRequest;
import com.example.demo.mapper.PersonMapper;
import com.example.demo.mapper.RelationshipSuggestionActionMapper;
import com.example.demo.pojo.Person;
import com.example.demo.pojo.RelationshipSuggestionAction;
import com.example.demo.service.RelationshipSuggestionActionService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Service
public class RelationshipSuggestionActionServiceImpl implements RelationshipSuggestionActionService {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    @Autowired
    private RelationshipSuggestionActionMapper actionMapper;

    @Autowired
    private PersonMapper personMapper;

    @PostConstruct
    public void initTable() {
        actionMapper.createTableIfNotExists();
    }

    @Override
    public List<RelationshipSuggestionAction> list(Long userId, String status, Long personId, Integer limit) {
        return actionMapper.findByUserId(userId, normalizeStatusFilter(status), personId, normalizeLimit(limit));
    }

    @Override
    public RelationshipSuggestionAction createOrUpdate(Long userId, RelationshipSuggestionActionRequest request) {
        RelationshipSuggestionAction incoming = buildAction(userId, request);
        RelationshipSuggestionAction existing = actionMapper.findBySuggestionKey(userId, incoming.getSuggestionKey());
        if (existing == null) {
            actionMapper.insert(incoming);
            return requireById(userId, incoming.getId());
        }

        mergeExisting(existing, incoming);
        actionMapper.update(existing);
        return requireById(userId, existing.getId());
    }

    @Override
    public RelationshipSuggestionAction update(Long userId, Long id, RelationshipSuggestionActionUpdateRequest request) {
        RelationshipSuggestionAction existing = requireById(userId, id);
        String status = normalizeStatus(request == null ? null : request.getStatus(), existing.getStatus());
        existing.setStatus(status);
        existing.setOutcome(clean(request == null ? null : request.getOutcome()));
        existing.setFeedback(clean(request == null ? null : request.getFeedback()));
        existing.setHelpfulnessRating(normalizeRating(request == null ? null : request.getHelpfulnessRating()));
        applyStatusTimes(existing);

        actionMapper.update(existing);
        return requireById(userId, id);
    }

    @Override
    public List<RelationshipSuggestionAction> recentFeedback(Long userId, Integer limit) {
        return actionMapper.findRecentFeedback(userId, normalizeLimit(limit));
    }

    private RelationshipSuggestionAction buildAction(Long userId, RelationshipSuggestionActionRequest request) {
        if (request == null) {
            throw new RuntimeException("建议行动不能为空");
        }

        RelationshipSuggestionAction action = new RelationshipSuggestionAction();
        action.setUserId(userId);
        action.setPersonId(request.getPersonId());
        action.setPersonName(resolvePersonName(userId, request.getPersonId(), request.getPersonName()));
        action.setPriority(normalizePriority(request.getPriority()));
        action.setTitle(required(request.getTitle(), "建议标题不能为空"));
        action.setReason(clean(request.getReason()));
        action.setAction(required(request.getAction(), "建议行动不能为空"));
        action.setGeneratedBy(normalizeGeneratedBy(request.getGeneratedBy()));
        action.setSuggestionKey(buildSuggestionKey(action));
        action.setStatus(normalizeStatus(request.getStatus(), "PLANNED"));
        action.setOutcome(clean(request.getOutcome()));
        action.setFeedback(clean(request.getFeedback()));
        action.setHelpfulnessRating(normalizeRating(request.getHelpfulnessRating()));
        applyStatusTimes(action);
        return action;
    }

    private void mergeExisting(RelationshipSuggestionAction existing, RelationshipSuggestionAction incoming) {
        existing.setPersonId(incoming.getPersonId());
        existing.setPersonName(incoming.getPersonName());
        existing.setPriority(incoming.getPriority());
        existing.setTitle(incoming.getTitle());
        existing.setReason(incoming.getReason());
        existing.setAction(incoming.getAction());
        existing.setGeneratedBy(incoming.getGeneratedBy());
        existing.setStatus(incoming.getStatus());
        existing.setOutcome(incoming.getOutcome());
        existing.setFeedback(incoming.getFeedback());
        existing.setHelpfulnessRating(incoming.getHelpfulnessRating());

        if (existing.getPlannedAt() == null) {
            existing.setPlannedAt(incoming.getPlannedAt());
        }
        if (incoming.getExecutedAt() != null) {
            existing.setExecutedAt(incoming.getExecutedAt());
        }
    }

    private RelationshipSuggestionAction requireById(Long userId, Long id) {
        RelationshipSuggestionAction action = actionMapper.findByIdAndUserId(id, userId);
        if (action == null) {
            throw new RuntimeException("建议行动记录不存在");
        }
        return action;
    }

    private String resolvePersonName(Long userId, Long personId, String personName) {
        if (personId != null) {
            Person person = personMapper.findByIdAndUserId(personId, userId);
            if (person == null) {
                throw new RuntimeException("人物不存在");
            }
            return person.getName();
        }

        String cleanName = clean(personName);
        if (cleanName == null || cleanName.isEmpty()) {
            throw new RuntimeException("建议关联人物不能为空");
        }
        return truncate(cleanName, 50);
    }

    private void applyStatusTimes(RelationshipSuggestionAction action) {
        String status = normalizeStatus(action.getStatus(), "PLANNED");
        LocalDateTime now = LocalDateTime.now();
        if ("PLANNED".equals(status) && action.getPlannedAt() == null) {
            action.setPlannedAt(now);
        }
        if ("DONE".equals(status) && action.getExecutedAt() == null) {
            action.setExecutedAt(now);
        }
    }

    private String buildSuggestionKey(RelationshipSuggestionAction action) {
        String raw = String.join("|",
                String.valueOf(action.getPersonId() == null ? "" : action.getPersonId()),
                value(action.getPersonName()),
                value(action.getTitle()),
                value(action.getAction()),
                value(action.getGeneratedBy())
        );

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return Integer.toHexString(raw.hashCode());
        }
    }

    private String normalizeStatusFilter(String status) {
        if (status == null || status.trim().isEmpty()) {
            return "";
        }
        return normalizeStatus(status, "PLANNED");
    }

    private String normalizeStatus(String status, String fallback) {
        String value = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        return switch (value) {
            case "PLANNED", "DONE", "SKIPPED" -> value;
            default -> fallback;
        };
    }

    private String normalizePriority(String priority) {
        String value = priority == null ? "" : priority.trim().toUpperCase(Locale.ROOT);
        return switch (value) {
            case "HIGH", "MEDIUM", "LOW" -> value;
            default -> "MEDIUM";
        };
    }

    private String normalizeGeneratedBy(String generatedBy) {
        String value = generatedBy == null ? "" : generatedBy.trim().toUpperCase(Locale.ROOT);
        return switch (value) {
            case "AI", "RULE" -> value;
            default -> "RULE";
        };
    }

    private Integer normalizeRating(Integer rating) {
        if (rating == null) {
            return null;
        }
        if (rating < 1 || rating > 5) {
            throw new RuntimeException("建议帮助评分必须在 1 到 5 之间");
        }
        return rating;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private String required(String value, String message) {
        String cleanValue = clean(value);
        if (cleanValue == null || cleanValue.isEmpty()) {
            throw new RuntimeException(message);
        }
        return cleanValue;
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        return value.trim();
    }

    private String value(String value) {
        return value == null ? "" : value.trim();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
