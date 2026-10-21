package com.example.demo.service;

import com.example.demo.dto.RelationshipSuggestionActionRequest;
import com.example.demo.dto.RelationshipSuggestionActionUpdateRequest;
import com.example.demo.pojo.RelationshipSuggestionAction;

import java.util.List;

public interface RelationshipSuggestionActionService {
    List<RelationshipSuggestionAction> list(Long userId, String status, Long personId, Integer limit);

    RelationshipSuggestionAction createOrUpdate(Long userId, RelationshipSuggestionActionRequest request);

    RelationshipSuggestionAction update(Long userId, Long id, RelationshipSuggestionActionUpdateRequest request);

    List<RelationshipSuggestionAction> recentFeedback(Long userId, Integer limit);
}
