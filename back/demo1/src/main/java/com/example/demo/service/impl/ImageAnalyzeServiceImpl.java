package com.example.demo.service.impl;

import com.example.demo.dto.ImageAnalyzeResponse;
import com.example.demo.dto.MemoryCandidate;
import com.example.demo.dto.MemoryExtractionResponse;
import com.example.demo.service.ImageAnalyzeService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

@Service
public class ImageAnalyzeServiceImpl implements ImageAnalyzeService {

    private static final Logger log = LoggerFactory.getLogger(ImageAnalyzeServiceImpl.class);

    private static final List<String> SUPPORTED_CONTENT_TYPES = List.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/bmp",
            "image/gif"
    );

    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    @Value("${app.vision.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String baseUrl;

    @Value("${app.vision.api-key:${DASHSCOPE_API_KEY:}}")
    private String apiKey;

    @Value("${app.vision.model-name:qwen-vl-plus}")
    private String modelName;

    @Value("${app.vision.timeout-seconds:90}")
    private Integer timeoutSeconds;

    @Value("${app.vision.max-file-size:10485760}")
    private Long maxFileSize;

    public ImageAnalyzeServiceImpl() {
        this.objectMapper = new ObjectMapper();
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(90));
        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Override
    public ImageAnalyzeResponse analyze(MultipartFile file, String prompt) {
        validateFile(file);
        validateConfig();

        String finalPrompt = buildPrompt(prompt);

        try {
            String answer = requestVisionAnswer(file, finalPrompt);
            return new ImageAnalyzeResponse(answer, modelName, file.getOriginalFilename());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("图片分析失败，请稍后再试");
        }
    }

    @Override
    public MemoryExtractionResponse extractMemoryCandidates(MultipartFile file, String personName) {
        validateFile(file);
        validateConfig();

        String finalPrompt = buildMemoryExtractionPrompt(personName);

        try {
            String rawAnswer = requestVisionAnswer(file, finalPrompt);
            return parseMemoryExtraction(rawAnswer, file.getOriginalFilename(), personName);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("图片记忆提取失败，请稍后再试");
        }
    }

    private String requestVisionAnswer(MultipartFile file, String prompt) throws Exception {
        String dataUrl = toDataUrl(file);
        ObjectNode requestBody = buildRequestBody(dataUrl, prompt);
        String requestJson = objectMapper.writeValueAsString(requestBody);

        String responseBody = webClient.post()
                .uri(baseUrl + "/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestJson)
                .retrieve()
                .onStatus(status -> status.isError(), response -> response.bodyToMono(String.class)
                        .flatMap(body -> Mono.error(new RuntimeException("图片分析失败：" + body))))
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(timeoutSeconds));

        return parseAnswer(responseBody);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("图片不能为空");
        }

        if (file.getSize() > maxFileSize) {
            throw new RuntimeException("图片不能超过10MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !SUPPORTED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new RuntimeException("当前只支持 jpg、png、webp、bmp、gif 图片");
        }
    }

    private void validateConfig() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new RuntimeException("未配置视觉模型API Key");
        }
    }

    private String buildPrompt(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            return "请用中文分析这张图片，说明图片中的主要内容；如果图片里有文字，请尽量提取出来。";
        }
        return prompt.trim();
    }

    private String buildMemoryExtractionPrompt(String personName) {
        String target = personName == null || personName.trim().isEmpty()
                ? "暂未指定人物，请从图片文字中识别可能的人物姓名；如果无法确认，personName 返回空字符串。"
                : "当前用户指定的人物是：" + personName.trim() + "。不要把其他人猜成这个人物。";

        return """
                你是一个关系记忆提取器。请从图片中提取与人物相关、能够被图片内容直接支持的事实。
                %s

                只返回一个合法 JSON 对象，不要返回 Markdown，不要加 ```json 标记，不要解释 JSON。
                JSON 格式必须是：
                {
                  "personName": "人物姓名或空字符串",
                  "memories": [
                    {
                      "memoryType": "HOBBY",
                      "content": "明确的事实",
                      "confidence": 0.0
                    }
                  ]
                }

                memoryType 只能使用：
                HOBBY=爱好或偏好，QUOTE=明确说过的话，TRAIT=明确表现出的性格特点，
                DISLIKE=明确不喜欢的事物，DATE=重要日期，NOTE=其他备注。

                规则：
                1. 只提取图片中明确出现或能够直接确认的信息，不要根据外貌、语气或常识猜测。
                2. 最多返回 8 条记忆，没有可靠信息时 memories 返回空数组。
                3. confidence 必须是 0 到 1 之间的小数，表示你对这条候选记忆的把握。
                4. 每条 content 要简洁、完整，适合直接保存到人物记忆中。
                """.formatted(target);
    }

    private MemoryExtractionResponse parseMemoryExtraction(String rawAnswer,
                                                           String filename,
                                                           String requestedPersonName) {
        String personName = requestedPersonName == null ? "" : requestedPersonName.trim();
        List<MemoryCandidate> candidates = new ArrayList<>();

        try {
            JsonNode root = readMemoryJson(rawAnswer);

            String detectedName = root.path("personName").asText("").trim();
            if (!detectedName.isEmpty()) {
                personName = detectedName;
            }

            JsonNode payload = root.path("data").isObject() ? root.path("data") : root;
            JsonNode memories = firstArray(payload, "memories", "candidates", "items");

            if (memories.isArray()) {
                for (JsonNode item : memories) {
                    String content = firstText(item, "content", "memoryContent", "text", "value");
                    if (content.isEmpty()) {
                        continue;
                    }

                    String memoryType = normalizeMemoryType(item.path("memoryType").asText(""));
                    double confidence = parseConfidence(item.path("confidence"));
                    candidates.add(new MemoryCandidate(memoryType, content, confidence));

                    if (candidates.size() >= 8) {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            // 保留 rawAnswer，让前端仍然可以查看模型原始结果并手动录入。
            log.warn("无法解析视觉模型返回的候选记忆 JSON: {}", e.getMessage());
        }

        return new MemoryExtractionResponse(
                personName,
                candidates,
                modelName,
                filename,
                rawAnswer
        );
    }

    /**
     * 兼容视觉模型常见的几种返回形式：
     * 1. ```json ... ```
     * 2. 直接返回 JSON 对象
     * 3. [{"type":"text","text":"```json ..."}]
     */
    private JsonNode readMemoryJson(String rawAnswer) throws Exception {
        String text = rawAnswer == null ? "" : rawAnswer.trim();

        JsonNode direct = tryReadJson(text);
        if (direct != null && !direct.isArray()) {
            if (hasMemoryFields(direct)) {
                return direct;
            }

            String nestedText = extractTextFromArray(direct.path("content"));
            JsonNode nested = tryReadJson(nestedText);
            if (nested != null) {
                return nested;
            }
        }

        if (direct != null && direct.isArray()) {
            String contentText = extractTextFromArray(direct);
            JsonNode nested = tryReadJson(contentText);
            if (nested != null) {
                return nested;
            }
        }

        JsonNode parsed = tryReadJson(extractJson(text));
        if (parsed != null) {
            return parsed;
        }

        throw new IllegalArgumentException("模型返回内容不是合法 JSON");
    }

    private JsonNode tryReadJson(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        String cleanText = text.trim();

        try {
            return objectMapper.readTree(cleanText);
        } catch (Exception firstError) {
            /*
             * 模型偶尔会在数组或对象最后多写一个逗号。
             * 只做这一项保守修复，不改变正常 JSON 的内容。
             */
            String fixedText = cleanText.replaceAll(",\\s*([}\\]])", "$1");
            if (fixedText.equals(cleanText)) {
                return null;
            }

            try {
                return objectMapper.readTree(fixedText);
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    private boolean hasMemoryFields(JsonNode node) {
        return node != null
                && (node.has("memories") || node.has("candidates") || node.has("items"));
    }

    private JsonNode firstArray(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.path(fieldName);
            if (value.isArray()) {
                return value;
            }
        }
        return objectMapper.createArrayNode();
    }

    private String firstText(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.path(fieldName);
            if (value.isTextual()) {
                String text = value.asText().trim();
                if (!text.isEmpty()) {
                    return text;
                }
            }
        }
        return "";
    }

    private String extractTextFromArray(JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray()) {
            return "";
        }

        StringBuilder text = new StringBuilder();
        for (JsonNode item : arrayNode) {
            if (item.isTextual()) {
                text.append(item.asText());
                continue;
            }

            String itemText = firstText(item, "text", "content");
            if (!itemText.isEmpty()) {
                text.append(itemText);
            }
        }
        return text.toString().trim();
    }

    private String extractJson(String rawAnswer) {
        if (rawAnswer == null) {
            return "{}";
        }

        String text = rawAnswer.trim()
                .replaceFirst("^```(?:json)?\\s*", "")
                .replaceFirst("\\s*```$", "")
                .trim();
        int objectStart = text.indexOf('{');
        if (objectStart < 0) {
            return text;
        }

        int depth = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int index = objectStart; index < text.length(); index++) {
            char current = text.charAt(index);

            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == '"') {
                    inString = false;
                }
                continue;
            }

            if (current == '"') {
                inString = true;
            } else if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return text.substring(objectStart, index + 1);
                }
            }
        }

        return text.substring(objectStart);
    }

    private String normalizeMemoryType(String value) {
        String type = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        return switch (type) {
            case "HOBBY", "QUOTE", "TRAIT", "DISLIKE", "DATE", "NOTE" -> type;
            case "爱好", "兴趣", "偏好" -> "HOBBY";
            case "语录", "说过的话", "原话" -> "QUOTE";
            case "性格", "性格特点", "特点" -> "TRAIT";
            case "不喜欢", "讨厌" -> "DISLIKE";
            case "日期", "重要日期", "生日", "纪念日" -> "DATE";
            default -> "NOTE";
        };
    }

    private double parseConfidence(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return 0.7D;
        }

        double value;
        if (node.isNumber()) {
            value = node.asDouble();
        } else {
            String text = node.asText("").replace("%", "").trim();
            try {
                value = Double.parseDouble(text);
                if (value > 1) {
                    value = value / 100D;
                }
            } catch (NumberFormatException e) {
                value = 0.7D;
            }
        }

        return Math.max(0D, Math.min(1D, value));
    }

    private String toDataUrl(MultipartFile file) throws Exception {
        String contentType = file.getContentType().toLowerCase();
        String base64 = Base64.getEncoder().encodeToString(file.getBytes());
        return "data:" + contentType + ";base64," + base64;
    }

    private ObjectNode buildRequestBody(String dataUrl, String prompt) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", modelName);
        root.put("temperature", 0.2);

        ArrayNode messages = root.putArray("messages");
        ObjectNode userMessage = messages.addObject();
        userMessage.put("role", "user");

        ArrayNode content = userMessage.putArray("content");
        ObjectNode textNode = content.addObject();
        textNode.put("type", "text");
        textNode.put("text", prompt);

        ObjectNode imageNode = content.addObject();
        imageNode.put("type", "image_url");
        ObjectNode imageUrl = imageNode.putObject("image_url");
        imageUrl.put("url", dataUrl);

        return root;
    }

    private String parseAnswer(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode content = root.path("choices").path(0).path("message").path("content");

        if (content.isMissingNode() || content.isNull()) {
            throw new RuntimeException("图片分析失败：模型没有返回内容");
        }

        if (content.isTextual()) {
            return content.asText();
        }

        if (content.isArray()) {
            String text = extractTextFromArray(content);
            if (!text.isEmpty()) {
                return text;
            }
        }

        return content.toString();
    }
}
