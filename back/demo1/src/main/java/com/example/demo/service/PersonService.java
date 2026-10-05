package com.example.demo.service;

import com.example.demo.dto.PageResult;
import com.example.demo.pojo.Person;

import java.util.List;

public interface PersonService {

    List<Person> list(Long userId);

    PageResult<Person> page(Long userId, Integer page, Integer pageSize);

    Person getById(Long id, Long userId);

    void add(Person person);

    void update(Person person);

    void delete(Long id, Long userId);
}
