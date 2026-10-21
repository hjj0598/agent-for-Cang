package com.example.demo.dto;

import com.example.demo.pojo.PersonMemory;
import com.example.demo.pojo.PersonMemoryRelation;
import lombok.Data;

import java.util.List;

@Data
public class MemoryConflictResult {

    private String status;
    private String message;
    private String reason;
    private String conflictKind;
    private String suggestedAction;
    private PersonMemory conflictMemory;
    private PersonMemory savedMemory;
    private List<PersonMemoryRelation> memoryRelations;

    public static MemoryConflictResult saved(PersonMemory savedMemory) {
        MemoryConflictResult result = new MemoryConflictResult();
        result.setStatus("SAVED");
        result.setMessage("记忆已保存");
        result.setSavedMemory(savedMemory);
        return result;
    }

    public static MemoryConflictResult conflict(PersonMemory conflictMemory, String reason) {
        MemoryConflictResult result = new MemoryConflictResult();
        result.setStatus("CONFLICT");
        result.setMessage("检测到可能冲突的历史记忆，请确认处理方式");
        result.setConflictMemory(conflictMemory);
        result.setReason(reason);
        result.setConflictKind("CONTRADICTION");
        result.setSuggestedAction("REPLACE_OLD");
        return result;
    }

    public static MemoryConflictResult change(PersonMemory conflictMemory, String reason) {
        MemoryConflictResult result = new MemoryConflictResult();
        result.setStatus("CHANGE");
        result.setMessage("检测到可能的人物记忆变化，请确认处理方式");
        result.setConflictMemory(conflictMemory);
        result.setReason(reason);
        result.setConflictKind("TEMPORAL_CHANGE");
        result.setSuggestedAction("KEEP_BOTH");
        return result;
    }
}
