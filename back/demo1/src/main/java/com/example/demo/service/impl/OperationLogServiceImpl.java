package com.example.demo.service.impl;

import com.example.demo.dto.PageResult;
import com.example.demo.mapper.OperationLogMapper;
import com.example.demo.pojo.OperationLog;
import com.example.demo.service.OperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OperationLogServiceImpl implements OperationLogService {

    @Autowired
    private OperationLogMapper operationLogMapper;

    @Override
    public void record(Long userId, String operationType, String operationContent) {
        if (userId == null) {
            return;
        }

        OperationLog operationLog = new OperationLog();
        operationLog.setUserId(userId);
        operationLog.setOperationType(operationType);
        operationLog.setOperationContent(operationContent);

        operationLogMapper.insert(operationLog);
    }

    @Override
    public List<OperationLog> recent(Long userId, Integer limit) {
        int safeLimit = limit == null || limit < 1 ? 10 : limit;
        safeLimit = Math.min(safeLimit, 50);
        return operationLogMapper.findRecentByUserId(userId, safeLimit);
    }

    @Override
    public List<OperationLog> recentAll(Integer limit) {
        int safeLimit = limit == null || limit < 1 ? 10 : limit;
        safeLimit = Math.min(safeLimit, 50);
        return operationLogMapper.findRecentAll(safeLimit);
    }

    @Override
    public PageResult<OperationLog> pageAll(Integer page, Integer pageSize, Long userId, String keyword) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        safePageSize = Math.min(safePageSize, 50);
        String safeKeyword = keyword == null ? "" : keyword.trim();

        int offset = (safePage - 1) * safePageSize;
        Long total = operationLogMapper.countPageAll(userId, safeKeyword);
        List<OperationLog> rows = operationLogMapper.findPageAll(userId, safeKeyword, offset, safePageSize);

        return new PageResult<>(total, rows);
    }
}
