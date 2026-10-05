package com.example.demo.tool;

import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class TimeTool {

    @Tool("获取当前时间")
    public String getCurrentTime() {
        return LocalDateTime.now().toString();
    }
}
//