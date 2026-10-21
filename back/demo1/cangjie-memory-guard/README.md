# cangjie-memory-guard

This is a small Cangjie sidecar module for the Java relationship-memory backend.

It does not replace the Java Spring Boot service. It only provides a rule-based
guard endpoint for candidate memory creation:

```text
POST http://127.0.0.1:9001/analyze-memory
```

Request:

```json
{"content":"JJKING 最近不喜欢喝咖啡了"}
```

Response:

```json
{
  "memoryType": "DISLIKE",
  "trustLevel": "MEDIUM",
  "factType": "AI_EXTRACTED_FACT",
  "confidence": 0.86,
  "riskLevel": "HIGH",
  "needsReview": true,
  "hasTemporalChange": true,
  "hasConflictRisk": true,
  "reason": "仓颉规则检测：包含时间变化词和否定偏好词，可能是偏好变化或冲突记忆，建议进入候选记忆收件箱由用户确认。"
}
```

## Run

Install the Cangjie SDK first, then check:

```powershell
cjc --version
cjpm --version
```

If your SDK does not include `stdx`, download `cangjie_stdx` and configure the
real `path-option` in `cjpm.toml`.

Then run:

```powershell
cd D:\Java_project\agent\demo1\cangjie-memory-guard
cjpm run
```

After it starts, enable the Java integration:

```properties
app.cangjie.memory-guard.enabled=true
app.cangjie.memory-guard.url=http://localhost:9001/analyze-memory
app.cangjie.memory-guard.timeout-ms=800
```
