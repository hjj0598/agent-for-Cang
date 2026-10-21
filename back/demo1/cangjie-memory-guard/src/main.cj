import stdx.net.http.*
import stdx.log.*

let server = ServerBuilder()
    .addr("127.0.0.1")
    .port(9001)
    .build()

func containsAny(text: String, words: Array<String>): Bool {
    for (word in words) {
        if (text.contains(word)) {
            return true
        }
    }
    return false
}

func hasTemporalWord(text: String): Bool {
    return containsAny(text, ["最近", "现在", "目前", "以后", "不再", "开始", "改成", "变成", "以前", "过去"])
}

func hasNegativeWord(text: String): Bool {
    return containsAny(text, ["不喜欢", "讨厌", "避免", "别", "不要", "不想", "拒绝", "反感"])
}

func hasPreferenceWord(text: String): Bool {
    return containsAny(text, ["喜欢", "爱好", "偏好", "想要", "希望", "习惯", "常去", "经常"])
}

func hasDateWord(text: String): Bool {
    return containsAny(text, ["生日", "纪念日", "日期", "时间", "月", "号", "日", "年"])
}

func hasRelationshipWord(text: String): Bool {
    return containsAny(text, ["朋友", "同学", "室友", "家人", "同事", "对象", "妈妈", "爸爸", "哥哥", "姐姐"])
}

func hasQuoteWord(text: String): Bool {
    return containsAny(text, ["说", "表示", "提到", "原话", "告诉我", "他说", "她说"])
}

func hasTraitWord(text: String): Bool {
    return containsAny(text, ["性格", "内向", "外向", "敏感", "开朗", "慢热", "理性", "情绪"])
}

func inferMemoryType(text: String): String {
    if (hasDateWord(text)) {
        return "DATE"
    }
    if (hasNegativeWord(text)) {
        return "DISLIKE"
    }
    if (hasPreferenceWord(text)) {
        return "HOBBY"
    }
    if (hasRelationshipWord(text)) {
        return "RELATIONSHIP"
    }
    if (hasQuoteWord(text)) {
        return "QUOTE"
    }
    if (hasTraitWord(text)) {
        return "TRAIT"
    }
    return "NOTE"
}

func inferTrustLevel(confidence: Float64, hasTemporal: Bool, hasNegative: Bool): String {
    if (hasTemporal && hasNegative) {
        return "MEDIUM"
    }
    if (confidence >= 0.82) {
        return "HIGH"
    }
    if (confidence >= 0.60) {
        return "MEDIUM"
    }
    return "LOW"
}

func inferRiskLevel(hasTemporal: Bool, hasNegative: Bool, memoryType: String): String {
    if (hasTemporal && hasNegative) {
        return "HIGH"
    }
    if (hasTemporal || memoryType == "DATE" || memoryType == "RELATIONSHIP") {
        return "MEDIUM"
    }
    return "LOW"
}

func inferConfidence(text: String, memoryType: String, hasTemporal: Bool, hasNegative: Bool): Float64 {
    var confidence = 0.62
    if (memoryType != "NOTE") {
        confidence += 0.12
    }
    if (hasTemporal) {
        confidence += 0.06
    }
    if (hasNegative) {
        confidence += 0.06
    }
    if (text.size >= 8) {
        confidence += 0.06
    }
    if (confidence > 0.95) {
        confidence = 0.95
    }
    return confidence
}

func buildReason(memoryType: String, riskLevel: String, hasTemporal: Bool, hasNegative: Bool): String {
    if (riskLevel == "HIGH") {
        return "仓颉规则检测：包含时间变化词和否定偏好词，可能是偏好变化或冲突记忆，建议进入候选记忆收件箱由用户确认。"
    }
    if (hasTemporal) {
        return "仓颉规则检测：包含时间变化信息，建议作为可追溯候选记忆处理。"
    }
    if (hasNegative) {
        return "仓颉规则检测：包含否定偏好信息，建议降低自动写入风险。"
    }
    if (memoryType != "NOTE") {
        return "仓颉规则检测：识别到明确记忆类型，可作为候选记忆补全标签。"
    }
    return "仓颉规则检测：未发现明显高风险信号，保持普通候选记忆处理。"
}

func boolText(value: Bool): String {
    if (value) {
        return "true"
    }
    return "false"
}

func escapeJson(value: String): String {
    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\r", "\\r")
        .replace("\n", "\\n")
}

func extractContent(body: String): String {
    let compact = body.replace("\r", "").replace("\n", "").replace(" ", "")
    let marker = "\"content\":\""
    if (!compact.contains(marker)) {
        return body
    }

    let parts = compact.split(marker)
    if (parts.size < 2) {
        return body
    }

    let tail = parts[1]
    let endParts = tail.split("\"")
    if (endParts.size == 0) {
        return body
    }

    return endParts[0]
        .replace("\\\"", "\"")
        .replace("\\n", "\n")
        .replace("\\r", "\r")
        .replace("\\\\", "\\")
}

func readRequestBody(context: HttpContext): String {
    let buffer = Array<Byte>(65536, repeat: 0)
    let length = context.request.body.read(buffer)
    if (length <= 0) {
        return ""
    }
    return String.fromUtf8(buffer[..length])
}

func analyze(content: String): String {
    let memoryType = inferMemoryType(content)
    let hasTemporal = hasTemporalWord(content)
    let hasNegative = hasNegativeWord(content)
    let conflictRisk = hasTemporal && hasNegative
    let confidence = inferConfidence(content, memoryType, hasTemporal, hasNegative)
    let trustLevel = inferTrustLevel(confidence, hasTemporal, hasNegative)
    let riskLevel = inferRiskLevel(hasTemporal, hasNegative, memoryType)
    let needsReview = riskLevel != "LOW"
    let reason = buildReason(memoryType, riskLevel, hasTemporal, hasNegative)

    return "{" +
        "\"memoryType\":\"${memoryType}\"," +
        "\"trustLevel\":\"${trustLevel}\"," +
        "\"factType\":\"AI_EXTRACTED_FACT\"," +
        "\"confidence\":${confidence}," +
        "\"riskLevel\":\"${riskLevel}\"," +
        "\"needsReview\":${boolText(needsReview)}," +
        "\"hasTemporalChange\":${boolText(hasTemporal)}," +
        "\"hasConflictRisk\":${boolText(conflictRisk)}," +
        "\"reason\":\"${escapeJson(reason)}\"" +
        "}"
}

func startServer(): Unit {
    server.distributor.register("/health", { context =>
        context.responseBuilder
            .status(200)
            .header("Content-Type", "application/json; charset=utf-8")
            .body("{\"status\":\"UP\",\"module\":\"cangjie-memory-guard\"}")
    })

    server.distributor.register("/analyze-memory", { context =>
        if (context.request.method != "POST") {
            context.responseBuilder
                .status(405)
                .header("Content-Type", "application/json; charset=utf-8")
                .body("{\"error\":\"method_not_allowed\"}")
        } else {
            let body = readRequestBody(context)
            let content = extractContent(body)
            let response = analyze(content)
            context.responseBuilder
                .status(200)
                .header("Content-Type", "application/json; charset=utf-8")
                .body(response)
        }
    })

    server.logger.level = LogLevel.OFF
    println("cangjie-memory-guard listening on http://127.0.0.1:9001")
    server.serve()
}

main(): Unit {
    startServer()
}
