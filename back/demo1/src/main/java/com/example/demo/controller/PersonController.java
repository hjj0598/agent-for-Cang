package com.example.demo.controller;

import com.example.demo.dto.PageResult;
import com.example.demo.dto.PersonRequest;
import com.example.demo.pojo.Person;
import com.example.demo.pojo.Result;
import com.example.demo.service.OperationLogService;
import com.example.demo.service.PersonService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/persons")
public class PersonController {

    @Autowired
    private PersonService personService;

    @Autowired
    private OperationLogService operationLogService;

    @GetMapping
    public Result<PageResult<Person>> list(@RequestParam(defaultValue = "1") Integer page,
                                           @RequestParam(defaultValue = "10") Integer pageSize,
                                           HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(personService.page(userId, page, pageSize));
    }

    @GetMapping("/{id}")
    public Result<Person> detail(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(personService.getById(id, userId));
    }

    @PostMapping
    public Result<Void> add(@RequestBody @Valid PersonRequest personRequest,
                            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        Person person = new Person();
        person.setUserId(userId);
        person.setName(personRequest.getName());
        person.setRelation(personRequest.getRelation());
        person.setDescription(personRequest.getDescription());

        personService.add(person);
        operationLogService.record(userId, "新增人物", "新增人物：" + person.getName());
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @RequestBody @Valid PersonRequest personRequest,
                               HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        Person person = new Person();
        person.setId(id);
        person.setUserId(userId);
        person.setName(personRequest.getName());
        person.setRelation(personRequest.getRelation());
        person.setDescription(personRequest.getDescription());

        personService.update(person);
        operationLogService.record(userId, "修改人物", "修改人物：" + person.getName());
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Person person = personService.getById(id, userId);
        personService.delete(id, userId);
        operationLogService.record(userId, "删除人物", "删除人物：" + person.getName());
        return Result.success();
    }
}
