package com.example.demo.dto;

import com.example.demo.pojo.MemoryCandidateRecord;
import lombok.Data;

@Data
public class MemoryCandidateConfirmResult {

    private String status;
    private String message;
    private MemoryConflictResult memoryResult;
    private MemoryCandidateRecord candidate;

    public static MemoryCandidateConfirmResult of(MemoryConflictResult memoryResult,
                                                  MemoryCandidateRecord candidate) {
        MemoryCandidateConfirmResult result = new MemoryCandidateConfirmResult();
        result.setMemoryResult(memoryResult);
        result.setCandidate(candidate);

        if (memoryResult != null && ("CONFLICT".equals(memoryResult.getStatus()) || "CHANGE".equals(memoryResult.getStatus()))) {
            result.setStatus(memoryResult.getStatus());
            result.setMessage(memoryResult.getMessage());
        } else {
            result.setStatus("CONFIRMED");
            result.setMessage("候选记忆已确认并保存");
        }

        return result;
    }
}
