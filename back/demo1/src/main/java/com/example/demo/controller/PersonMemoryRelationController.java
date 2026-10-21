package com.example.demo.controller;

import com.example.demo.pojo.PersonMemoryRelation;
import com.example.demo.pojo.Result;
import com.example.demo.service.PersonMemoryRelationService;
import com.example.demo.service.PersonService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/persons/{personId}")
public class PersonMemoryRelationController {

    @Autowired
    private PersonMemoryRelationService relationService;

    @Autowired
    private PersonService personService;

    @GetMapping("/memory-relations")
    public Result<List<PersonMemoryRelation>> listByPerson(@PathVariable Long personId,
                                                           @RequestParam(required = false) Integer limit,
                                                           HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        personService.getById(personId, userId);
        return Result.success(relationService.listByPerson(userId, personId, limit));
    }

    @GetMapping("/memories/{memoryId}/relations")
    public Result<List<PersonMemoryRelation>> listByMemory(@PathVariable Long personId,
                                                           @PathVariable Long memoryId,
                                                           @RequestParam(required = false) Integer limit,
                                                           HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        personService.getById(personId, userId);
        return Result.success(relationService.listByMemory(userId, personId, memoryId, limit));
    }
}
