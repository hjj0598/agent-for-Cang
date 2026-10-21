package com.example.demo.service;

import com.example.demo.dto.MemoryConflictResult;
import com.example.demo.pojo.PersonMemory;

import java.util.List;

public interface PersonMemoryService {

    List<PersonMemory> list(Long userId, Long personId);

    void add(PersonMemory memory);

    MemoryConflictResult addWithConflictCheck(PersonMemory memory, String conflictAction, Long conflictMemoryId);

    void delete(Long userId, Long personId, Long memoryId);
    List<PersonMemory> findByPersonName(String personName);

    void addByPersonName(String personName, String memoryType, String content, String source);

    List<PersonMemory> findByUserIdAndPersonName(Long userId, String personName);

    List<PersonMemory> searchByUserIdAndPersonName(Long userId, String personName);

    MemoryConflictResult addByUserIdAndPersonName(Long userId, String personName, String memoryType, String content, String source);
}
