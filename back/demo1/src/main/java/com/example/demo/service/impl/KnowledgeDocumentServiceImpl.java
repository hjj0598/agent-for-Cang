package com.example.demo.service.impl;

import com.example.demo.dto.PageResult;
import com.example.demo.mapper.KnowledgeChunkMapper;
import com.example.demo.mapper.KnowledgeDocumentMapper;
import com.example.demo.pojo.KnowledgeChunk;
import com.example.demo.pojo.KnowledgeDocument;
import com.example.demo.service.KnowledgeDocumentService;
import com.example.demo.service.KnowledgeIngestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class KnowledgeDocumentServiceImpl implements KnowledgeDocumentService {

    @Autowired
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Autowired
    private KnowledgeChunkMapper knowledgeChunkMapper;

    @Autowired
    private KnowledgeIngestService knowledgeIngestService;

    @Override
    public List<KnowledgeDocument> list(Long userId) {
        return knowledgeDocumentMapper.findByUserId(userId);
    }

    @Override
    public PageResult<KnowledgeDocument> page(Long userId, Integer page, Integer pageSize) {
        return page(userId, page, pageSize, null);
    }

    @Override
    public PageResult<KnowledgeDocument> page(Long userId, Integer page, Integer pageSize, String keyword) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        safePageSize = Math.min(safePageSize, 50);

        int offset = (safePage - 1) * safePageSize;
        String cleanKeyword = keyword == null ? "" : keyword.trim();

        Long total = knowledgeDocumentMapper.countByUserIdAndTitle(userId, cleanKeyword);
        List<KnowledgeDocument> rows = knowledgeDocumentMapper.findPageByUserIdAndTitle(userId, cleanKeyword, offset, safePageSize);

        return new PageResult<>(total, rows);
    }

    @Override
    public KnowledgeDocument getById(Long id, Long userId) {
        KnowledgeDocument document = knowledgeDocumentMapper.findByIdAndUserId(id, userId);

        if (document == null) {
            throw new RuntimeException("知识文档不存在");
        }

        return document;
    }

    @Override
    public KnowledgeDocument getByFileHash(Long userId, String fileHash) {
        if (fileHash == null || fileHash.trim().isEmpty()) {
            return null;
        }

        return knowledgeDocumentMapper.findByUserIdAndFileHash(userId, fileHash);
    }

    @Override
    public List<KnowledgeChunk> listChunks(Long id, Long userId) {
        KnowledgeDocument document = knowledgeDocumentMapper.findByIdAndUserId(id, userId);

        if (document == null) {
            throw new RuntimeException("知识文档不存在");
        }

        return knowledgeChunkMapper.findByDocumentIdAndUserId(id, userId);
    }

    @Override
    public void add(KnowledgeDocument document) {
        if (document.getTitle() == null || document.getTitle().trim().isEmpty()) {
            throw new RuntimeException("标题不能为空");
        }

        if (document.getContent() == null || document.getContent().trim().isEmpty()) {
            throw new RuntimeException("内容不能为空");
        }

        if (document.getSource() == null || document.getSource().trim().isEmpty()) {
            document.setSource("手动添加");
        }

        knowledgeDocumentMapper.insert(document);
        knowledgeIngestService.ingest(document);
    }

    @Transactional
    @Override
    public void update(KnowledgeDocument document) {
        KnowledgeDocument exists = knowledgeDocumentMapper.findByIdAndUserId(document.getId(), document.getUserId());

        if (exists == null) {
            throw new RuntimeException("知识文档不存在");
        }

        if (document.getTitle() == null || document.getTitle().trim().isEmpty()) {
            throw new RuntimeException("标题不能为空");
        }

        if (document.getContent() == null || document.getContent().trim().isEmpty()) {
            throw new RuntimeException("内容不能为空");
        }

        document.setSource(exists.getSource());

        knowledgeIngestService.deleteByDocument(document.getId(), document.getUserId());
        knowledgeDocumentMapper.update(document);
        knowledgeIngestService.ingest(document);
    }

    @Override
    public void delete(Long id, Long userId) {
        knowledgeIngestService.deleteByDocument(id, userId);
        knowledgeDocumentMapper.deleteByIdAndUserId(id, userId);
    }
}
