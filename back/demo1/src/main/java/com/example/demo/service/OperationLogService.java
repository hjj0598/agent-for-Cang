package com.example.demo.service;

import com.example.demo.pojo.OperationLog;
import com.example.demo.dto.PageResult;

import java.util.List;

public interface OperationLogService {

    void record(Long userId, String operationType, String operationContent);

    List<OperationLog> recent(Long userId, Integer limit);

    List<OperationLog> recentAll(Integer limit);

    PageResult<OperationLog> pageAll(Integer page, Integer pageSize, Long userId, String keyword);
}
