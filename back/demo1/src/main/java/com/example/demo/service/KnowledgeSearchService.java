package com.example.demo.service;

import com.example.demo.dto.KnowledgeSearchResult;

public interface KnowledgeSearchService {

    KnowledgeSearchResult search(Long userId, String question);
}