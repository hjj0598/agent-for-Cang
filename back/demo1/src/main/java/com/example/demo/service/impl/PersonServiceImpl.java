package com.example.demo.service.impl;

import com.example.demo.dto.PageResult;
import com.example.demo.mapper.PersonMapper;
import com.example.demo.pojo.Person;
import com.example.demo.service.PersonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PersonServiceImpl implements PersonService {

    @Autowired
    private PersonMapper personMapper;

    @Override
    public List<Person> list(Long userId) {
        return personMapper.findByUserId(userId);
    }

    @Override
    public PageResult<Person> page(Long userId, Integer page, Integer pageSize) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        safePageSize = Math.min(safePageSize, 50);

        int offset = (safePage - 1) * safePageSize;

        Long total = personMapper.countByUserId(userId);
        List<Person> rows = personMapper.findPageByUserId(userId, offset, safePageSize);

        return new PageResult<>(total, rows);
    }

    @Override
    public Person getById(Long id, Long userId) {
        Person person = personMapper.findByIdAndUserId(id, userId);
        if (person == null) {
            throw new RuntimeException("人物不存在");
        }
        return person;
    }

    @Override
    public void add(Person person) {
        if (person.getName() == null || person.getName().trim().isEmpty()) {
            throw new RuntimeException("人物姓名不能为空");
        }

        Person exists = personMapper.findByUserIdAndName(person.getUserId(), person.getName());

        if (exists != null) {
            throw new RuntimeException("该人物已存在");
        }

        personMapper.insert(person);
    }

    @Override
    public void update(Person person) {
        Person exists = personMapper.findByIdAndUserId(person.getId(), person.getUserId());
        if (exists == null) {
            throw new RuntimeException("人物不存在");
        }
        personMapper.update(person);
    }

    @Override
    public void delete(Long id, Long userId) {
        personMapper.deleteByIdAndUserId(id, userId);
    }
}
