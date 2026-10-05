package com.example.demo.controller;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
//  用来测试redis是否链接上
@RestController
public class RedisTestController {

    private final StringRedisTemplate stringRedisTemplate;

    public RedisTestController(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @GetMapping("/redis/test")
    public String test() {
        stringRedisTemplate.opsForValue().set("hello", "redis");
        return stringRedisTemplate.opsForValue().get("hello");
    }
}