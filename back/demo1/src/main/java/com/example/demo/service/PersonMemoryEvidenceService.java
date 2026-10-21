package com.example.demo.service;

import com.example.demo.dto.PersonMemoryEvidenceResult;

public interface PersonMemoryEvidenceService {

    PersonMemoryEvidenceResult search(Long userId, String question);
}
