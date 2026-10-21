package com.example.demo.service;

import com.example.demo.pojo.PersonMemory;
import com.example.demo.pojo.PersonMemoryRelation;

import java.util.List;

public interface PersonMemoryRelationService {

    List<PersonMemoryRelation> analyzeAfterMemorySaved(PersonMemory savedMemory);

    PersonMemoryRelation recordRelation(PersonMemory oldMemory,
                                        PersonMemory newMemory,
                                        String relationType,
                                        String reason,
                                        Double confidence);

    List<PersonMemoryRelation> listByPerson(Long userId, Long personId, Integer limit);

    List<PersonMemoryRelation> listByMemory(Long userId, Long personId, Long memoryId, Integer limit);
}
