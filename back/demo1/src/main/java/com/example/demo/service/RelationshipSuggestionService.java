package com.example.demo.service;

import com.example.demo.dto.RelationshipSuggestion;

import java.util.List;

public interface RelationshipSuggestionService {

    List<RelationshipSuggestion> generate(Long userId);
}
