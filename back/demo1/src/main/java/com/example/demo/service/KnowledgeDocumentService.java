package com.example.demo.service;

import com.example.demo.dto.PageResult;
import com.example.demo.pojo.KnowledgeChunk;
import com.example.demo.pojo.KnowledgeDocument;

import java.util.List;

public interface KnowledgeDocumentService {

    List<KnowledgeDocument> list(Long userId);

    PageResult<KnowledgeDocument> page(Long userId, Integer page, Integer pageSize);

    PageResult<KnowledgeDocument> page(Long userId, Integer page, Integer pageSize, String keyword);

    KnowledgeDocument getById(Long id, Long userId);

    KnowledgeDocument getByFileHash(Long userId, String fileHash);

    List<KnowledgeChunk> listChunks(Long id, Long userId);

    void add(KnowledgeDocument document);

    void update(KnowledgeDocument document);

    void delete(Long id, Long userId);
}
