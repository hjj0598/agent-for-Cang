package com.example.demo.service.impl;

import com.example.demo.mapper.PersonMemoryMapper;
import com.example.demo.mapper.PersonMapper;
import com.example.demo.pojo.Person;
import com.example.demo.pojo.PersonMemory;
import com.example.demo.service.PersonMemoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PersonMemoryServiceImpl implements PersonMemoryService {

    @Autowired
    private PersonMemoryMapper personMemoryMapper;

    @Autowired
    private PersonMapper personMapper;

    @Override
    public List<PersonMemory> list(Long userId, Long personId) {
        if (personMapper.findByIdAndUserId(personId, userId) == null) {
            throw new RuntimeException("人物不存在");
        }

        return enrichSources(personMemoryMapper.findByPersonId(userId, personId));
    }

    @Override
    public void add(PersonMemory memory) {
        if (personMapper.findByIdAndUserId(memory.getPersonId(), memory.getUserId()) == null) {
            throw new RuntimeException("人物不存在");
        }

        if (memory.getMemoryType() == null || memory.getMemoryType().trim().isEmpty()) {
            memory.setMemoryType("NOTE");
        }

        if (memory.getSource() == null || memory.getSource().trim().isEmpty()) {
            memory.setSource("手动记录");
        }

        personMemoryMapper.insert(memory);
    }

    @Override
    public void delete(Long userId, Long personId, Long memoryId) {
        personMemoryMapper.deleteById(memoryId, personId, userId);
    }

    @Override
    public List<PersonMemory> findByPersonName(String personName) {
        return enrichSources(personMemoryMapper.findByPersonName(personName));
    }


    @Override
    public void addByPersonName(String personName, String memoryType, String content, String source) {
        if (personName == null || personName.trim().isEmpty()) {
            throw new RuntimeException("人物姓名不能为空");
        }

        if (content == null || content.trim().isEmpty()) {
            throw new RuntimeException("记忆内容不能为空");
        }

        Person person = personMapper.findByName(personName);

        if (person == null) {
            throw new RuntimeException("人物不存在，请先在人物管理中创建：" + personName);
        }

        PersonMemory memory = new PersonMemory();
        memory.setUserId(person.getUserId());
        memory.setPersonId(person.getId());
        memory.setMemoryType(memoryType == null || memoryType.trim().isEmpty() ? "NOTE" : memoryType);
        memory.setContent(content);
        memory.setSource(source == null || source.trim().isEmpty() ? "AI聊天记录" : source);

        personMemoryMapper.insert(memory);
    }



    @Override
    public List<PersonMemory> findByUserIdAndPersonName(Long userId, String personName) {
        return enrichSources(personMemoryMapper.findByUserIdAndPersonName(userId, personName));
    }

    @Override
    public List<PersonMemory> searchByUserIdAndPersonName(Long userId, String personName) {
        if (personName == null || personName.trim().isEmpty()) {
            throw new RuntimeException("搜索人物姓名不能为空");
        }

        return enrichSources(personMemoryMapper.searchByUserIdAndPersonName(userId, personName.trim()));
    }

    @Override
    public void addByUserIdAndPersonName(Long userId,
                                         String personName,
                                         String memoryType,
                                         String content,
                                         String source) {
        if (personName == null || personName.trim().isEmpty()) {
            throw new RuntimeException("人物姓名不能为空");
        }

        if (content == null || content.trim().isEmpty()) {
            throw new RuntimeException("记忆内容不能为空");
        }

        Person person = personMapper.findByUserIdAndName(userId, personName);

        if (person == null) {
            person = new Person();
            person.setUserId(userId);
            person.setName(personName);
            person.setRelation("未设置");
            person.setDescription("AI聊天中自动创建的人物");

            personMapper.insert(person);

            person = personMapper.findByUserIdAndName(userId, personName);
        }

        PersonMemory memory = new PersonMemory();
        memory.setUserId(userId);
        memory.setPersonId(person.getId());
        memory.setMemoryType(memoryType == null || memoryType.trim().isEmpty() ? "NOTE" : memoryType);
        memory.setContent(content);
        memory.setSource(source == null || source.trim().isEmpty() ? "AI聊天记录" : source);

        personMemoryMapper.insert(memory);
    }

    /**
     * source 是历史表中已有的字符串字段。
     * 这里把它拆成前端容易展示的来源类型和引用，不改变数据库结构。
     */
    private List<PersonMemory> enrichSources(List<PersonMemory> memories) {
        if (memories == null || memories.isEmpty()) {
            return memories == null ? new ArrayList<>() : memories;
        }

        memories.forEach(this::enrichSource);
        return memories;
    }

    private void enrichSource(PersonMemory memory) {
        String source = memory.getSource();
        if (source == null || source.trim().isEmpty()) {
            memory.setSource("手动记录");
            memory.setSourceType("手动记录");
            memory.setSourceReference("");
            return;
        }

        String cleanSource = source.trim();
        String sourceType = "手动记录";
        String sourceReference = cleanSource;

        if (cleanSource.startsWith("图片提取")) {
            sourceType = "图片提取";
            sourceReference = afterSeparator(cleanSource);
        } else if (cleanSource.contains("AI聊天") || cleanSource.contains("AI chat")) {
            sourceType = "AI 对话";
            sourceReference = afterSeparator(cleanSource);
        } else if (cleanSource.startsWith("知识库")) {
            sourceType = "知识库";
            sourceReference = afterSeparator(cleanSource);
        }

        memory.setSourceType(sourceType);
        memory.setSourceReference(sourceReference);
    }

    private String afterSeparator(String source) {
        int chineseSeparator = source.indexOf('：');
        int asciiSeparator = source.indexOf(':');
        int separator = chineseSeparator >= 0 ? chineseSeparator : asciiSeparator;

        if (separator < 0 || separator + 1 >= source.length()) {
            return "";
        }

        return source.substring(separator + 1).trim();
    }
}
