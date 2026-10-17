package com.example.demo.controller;

import com.example.demo.dto.PersonMemoryRequest;
import com.example.demo.pojo.Person;
import com.example.demo.pojo.PersonMemory;
import com.example.demo.pojo.Result;
import com.example.demo.service.OperationLogService;
import com.example.demo.service.PersonMemoryService;
import com.example.demo.service.PersonService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/persons/{personId}/memories")
public class PersonMemoryController {

    @Autowired
    private PersonMemoryService personMemoryService;

    @Autowired
    private PersonService personService;

    @Autowired
    private OperationLogService operationLogService;

    @GetMapping
    public Result<List<PersonMemory>> list(@PathVariable Long personId,
                                           HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(personMemoryService.list(userId, personId));
    }

    @PostMapping
    public Result<Void> add(@PathVariable Long personId,
                            @RequestBody @Valid PersonMemoryRequest personMemoryRequest,
                            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        PersonMemory memory = new PersonMemory();
        memory.setUserId(userId);
        memory.setPersonId(personId);
        memory.setMemoryType(personMemoryRequest.getMemoryType());
        memory.setContent(personMemoryRequest.getContent());
        memory.setSource(personMemoryRequest.getSource());

        personMemoryService.add(memory);
        Person person = personService.getById(personId, userId);
        operationLogService.record(userId, "添加人物记忆", "添加记忆：" + person.getName() + " - " + memory.getMemoryType());

        return Result.success();
    }

    @DeleteMapping("/{memoryId}")
    public Result<Void> delete(@PathVariable Long personId,
                               @PathVariable Long memoryId,
                               HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        personMemoryService.delete(userId, personId, memoryId);
        Person person = personService.getById(personId, userId);
        operationLogService.record(userId, "删除人物记忆", "删除记忆：" + person.getName());

        return Result.success();
    }
}
