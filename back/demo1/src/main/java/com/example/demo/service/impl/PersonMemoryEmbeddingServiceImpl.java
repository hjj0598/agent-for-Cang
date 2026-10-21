package com.example.demo.service.impl;

import com.example.demo.dto.PersonMemoryEvidence;
import com.example.demo.mapper.PersonMemoryEmbeddingMapper;
import com.example.demo.mapper.PersonMapper;
import com.example.demo.pojo.Person;
import com.example.demo.pojo.PersonMemory;
import com.example.demo.pojo.PersonMemoryEmbedding;
import com.example.demo.service.PersonMemoryEmbeddingService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PersonMemoryEmbeddingServiceImpl implements PersonMemoryEmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(PersonMemoryEmbeddingServiceImpl.class);

    @Autowired
    private PersonMemoryEmbeddingMapper personMemoryEmbeddingMapper;

    @Autowired
    private PersonMapper personMapper;

    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    @Autowired
    private EmbeddingModel embeddingModel;

    @PostConstruct
    public void initTable() {
        personMemoryEmbeddingMapper.createTableIfNotExists();
    }

    @Override
    public void index(PersonMemory memory) {
        if (memory == null || memory.getId() == null || memory.getUserId() == null || memory.getPersonId() == null) {
            return;
        }

        if (personMemoryEmbeddingMapper.findByMemoryId(memory.getId()) != null) {
            return;
        }

        Person person = personMapper.findByIdAndUserId(memory.getPersonId(), memory.getUserId());
        if (person == null) {
            return;
        }

        index(memory.getUserId(), memory.getPersonId(), memory.getId(), buildEmbeddingText(person, memory));
    }

    @Override
    public void indexMissing(Long userId, List<PersonMemoryEvidence> memories) {
        if (memories == null || memories.isEmpty()) {
            return;
        }

        for (PersonMemoryEvidence memory : memories) {
            try {
                if (memory.getMemoryId() == null || personMemoryEmbeddingMapper.findByMemoryId(memory.getMemoryId()) != null) {
                    continue;
                }

                index(userId, memory.getPersonId(), memory.getMemoryId(), buildEmbeddingText(memory));
            } catch (Exception e) {
                log.warn("人物记忆向量索引补齐失败 memoryId={}: {}", memory.getMemoryId(), e.getMessage());
            }
        }
    }

    private void index(Long userId, Long personId, Long memoryId, String embeddingText) {
        TextSegment segment = TextSegment.from(embeddingText);
        Embedding embedding = embeddingModel.embed(segment).content();
        String embeddingId = embeddingStore.add(embedding, segment);

        PersonMemoryEmbedding mapping = new PersonMemoryEmbedding();
        mapping.setUserId(userId);
        mapping.setPersonId(personId);
        mapping.setMemoryId(memoryId);
        mapping.setEmbeddingId(embeddingId);
        mapping.setEmbeddingText(embeddingText);

        personMemoryEmbeddingMapper.insert(mapping);
    }

    private String buildEmbeddingText(Person person, PersonMemory memory) {
        return "人物：" + nullToEmpty(person.getName()) + "\n"
                + "关系：" + nullToEmpty(person.getRelation()) + "\n"
                + "记忆类型：" + nullToEmpty(memory.getMemoryType()) + "\n"
                + "记忆内容：" + nullToEmpty(memory.getContent()) + "\n"
                + "来源：" + nullToEmpty(memory.getSource());
    }

    private String buildEmbeddingText(PersonMemoryEvidence memory) {
        return "人物：" + nullToEmpty(memory.getPersonName()) + "\n"
                + "关系：" + nullToEmpty(memory.getRelation()) + "\n"
                + "记忆类型：" + nullToEmpty(memory.getMemoryType()) + "\n"
                + "记忆内容：" + nullToEmpty(memory.getContent()) + "\n"
                + "来源：" + nullToEmpty(memory.getSource());
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
