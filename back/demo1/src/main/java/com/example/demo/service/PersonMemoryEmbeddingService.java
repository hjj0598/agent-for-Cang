package com.example.demo.service;

import com.example.demo.dto.PersonMemoryEvidence;
import com.example.demo.pojo.PersonMemory;

import java.util.List;

public interface PersonMemoryEmbeddingService {

    void index(PersonMemory memory);

    void indexMissing(Long userId, List<PersonMemoryEvidence> memories);
}
