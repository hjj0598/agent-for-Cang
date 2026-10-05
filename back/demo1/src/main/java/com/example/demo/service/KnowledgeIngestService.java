package com.example.demo.service;

import com.example.demo.pojo.KnowledgeDocument;

public interface KnowledgeIngestService {

    void ingest(KnowledgeDocument document);

    void deleteByDocument(Long documentId, Long userId);
}