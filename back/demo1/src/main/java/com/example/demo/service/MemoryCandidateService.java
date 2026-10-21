package com.example.demo.service;

import com.example.demo.dto.MemoryCandidateBatchCreateRequest;
import com.example.demo.dto.MemoryCandidateConfirmRequest;
import com.example.demo.dto.MemoryCandidateConfirmResult;
import com.example.demo.dto.MemoryCandidateCreateRequest;
import com.example.demo.dto.MemoryCandidateRejectRequest;
import com.example.demo.dto.MemoryCandidateUpdateRequest;
import com.example.demo.pojo.MemoryCandidateRecord;

import java.util.List;

public interface MemoryCandidateService {

    List<MemoryCandidateRecord> list(Long userId, String status, Long personId, Integer limit);

    MemoryCandidateRecord getById(Long userId, Long id);

    MemoryCandidateRecord create(Long userId, MemoryCandidateCreateRequest request);

    List<MemoryCandidateRecord> createBatch(Long userId, MemoryCandidateBatchCreateRequest request);

    MemoryCandidateRecord update(Long userId, Long id, MemoryCandidateUpdateRequest request);

    MemoryCandidateConfirmResult confirm(Long userId, Long id, MemoryCandidateConfirmRequest request);

    MemoryCandidateRecord reject(Long userId, Long id, MemoryCandidateRejectRequest request);

    void delete(Long userId, Long id);
}
