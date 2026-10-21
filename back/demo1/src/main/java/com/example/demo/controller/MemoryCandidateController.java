package com.example.demo.controller;

import com.example.demo.dto.MemoryCandidateBatchCreateRequest;
import com.example.demo.dto.MemoryCandidateConfirmRequest;
import com.example.demo.dto.MemoryCandidateConfirmResult;
import com.example.demo.dto.MemoryCandidateCreateRequest;
import com.example.demo.dto.MemoryCandidateRejectRequest;
import com.example.demo.dto.MemoryCandidateUpdateRequest;
import com.example.demo.pojo.MemoryCandidateRecord;
import com.example.demo.pojo.Result;
import com.example.demo.service.MemoryCandidateService;
import com.example.demo.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/memory-candidates")
public class MemoryCandidateController {

    @Autowired
    private MemoryCandidateService memoryCandidateService;

    @Autowired
    private OperationLogService operationLogService;

    @GetMapping
    public Result<List<MemoryCandidateRecord>> list(@RequestParam(required = false) String status,
                                                    @RequestParam(required = false) Long personId,
                                                    @RequestParam(required = false) Integer limit,
                                                    HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(memoryCandidateService.list(userId, status, personId, limit));
    }

    @GetMapping("/{id}")
    public Result<MemoryCandidateRecord> detail(@PathVariable Long id,
                                                HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(memoryCandidateService.getById(userId, id));
    }

    @PostMapping
    public Result<MemoryCandidateRecord> create(@RequestBody @Valid MemoryCandidateCreateRequest createRequest,
                                                HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        MemoryCandidateRecord candidate = memoryCandidateService.create(userId, createRequest);
        operationLogService.record(userId, "创建候选记忆", "创建候选记忆：" + candidate.getContent());
        return Result.success(candidate);
    }

    @PostMapping("/batch")
    public Result<List<MemoryCandidateRecord>> createBatch(
            @RequestBody @Valid MemoryCandidateBatchCreateRequest batchCreateRequest,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<MemoryCandidateRecord> candidates = memoryCandidateService.createBatch(userId, batchCreateRequest);
        operationLogService.record(userId, "批量创建候选记忆", "候选记忆数量：" + candidates.size());
        return Result.success(candidates);
    }

    @PutMapping("/{id}")
    public Result<MemoryCandidateRecord> update(@PathVariable Long id,
                                                @RequestBody @Valid MemoryCandidateUpdateRequest updateRequest,
                                                HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        MemoryCandidateRecord candidate = memoryCandidateService.update(userId, id, updateRequest);
        operationLogService.record(userId, "修改候选记忆", "修改候选记忆：" + candidate.getContent());
        return Result.success(candidate);
    }

    @PostMapping("/{id}/confirm")
    public Result<MemoryCandidateConfirmResult> confirm(
            @PathVariable Long id,
            @RequestBody(required = false) MemoryCandidateConfirmRequest confirmRequest,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        MemoryCandidateConfirmResult result = memoryCandidateService.confirm(userId, id, confirmRequest);
        if (!"CONFLICT".equals(result.getStatus()) && !"CHANGE".equals(result.getStatus())) {
            operationLogService.record(userId, "确认候选记忆", "确认候选记忆ID：" + id);
        }
        return Result.success(result);
    }

    @PostMapping("/{id}/reject")
    public Result<MemoryCandidateRecord> reject(@PathVariable Long id,
                                                @RequestBody(required = false) MemoryCandidateRejectRequest rejectRequest,
                                                HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        MemoryCandidateRecord candidate = memoryCandidateService.reject(userId, id, rejectRequest);
        operationLogService.record(userId, "拒绝候选记忆", "拒绝候选记忆ID：" + id);
        return Result.success(candidate);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id,
                               HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        memoryCandidateService.delete(userId, id);
        operationLogService.record(userId, "删除候选记忆", "删除候选记忆ID：" + id);
        return Result.success();
    }
}
