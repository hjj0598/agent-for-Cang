package com.example.demo.controller;

import com.example.demo.pojo.PersonMemory;
import com.example.demo.pojo.Result;
import com.example.demo.service.PersonMemoryService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/person-memories")
public class PersonMemorySearchController {

    @Autowired
    private PersonMemoryService personMemoryService;

    @GetMapping("/search")
    public Result<List<PersonMemory>> search(@RequestParam String personName,
                                             HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(personMemoryService.searchByUserIdAndPersonName(userId, personName));
    }
}
