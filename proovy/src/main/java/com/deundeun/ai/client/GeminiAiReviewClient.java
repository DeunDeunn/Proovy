package com.deundeun.ai.client;

import com.deundeun.ai.config.GeminiProperties;
import com.deundeun.ai.dto.AiReviewAiResult;
import com.deundeun.ai.enums.AiReviewDecision;
import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty(name = "gemini.api.key")
public class GeminiAiReviewClient implements AiReviewClient {

    private static final int CONNECT_TIMEOUT_MILLIS = 10_000;
    private static final int READ_TIMEOUT_MILLIS = 60_000;

    private final GeminiProperties properties;
    private final RestClient geminiRestClient;
    private final RestClient imageRestClient;
    private final ObjectMapper objectMapper;
    private final String s3Bucket;
    private final boolean localImageUrlAllowed;

    public GeminiAiReviewClient(
            GeminiProperties properties,
            Environment environment,
            @Value("${aws.s3.bucket:}") String s3Bucket
    ) {
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
        this.s3Bucket = s3Bucket;
        this.localImageUrlAllowed = Arrays.asList(environment.getActiveProfiles()).contains("local");

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        requestFactory.setReadTimeout(READ_TIMEOUT_MILLIS);

        this.geminiRestClient = RestClient.builder()
                .baseUrl(properties.apiBaseUrl())
                .requestFactory(requestFactory)
                .build();
        this.imageRestClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public AiReviewAiResult review(String prompt, List<String> imageUrls) {
        try {
            GeminiResponse response = geminiRestClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1beta/models/{model}:generateContent")
                            .queryParam("key", properties.apiKey())
                            .build(properties.modelName()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(createRequest(prompt, imageUrls))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        log.warn("Gemini API 실패 응답 - status={}", res.getStatusCode());
                        throw new ApiException(ErrorCode.SERVER_ERROR, "Gemini API 호출에 실패했습니다.");
                    })
                    .body(GeminiResponse.class);

            String text = extractText(response);
            String rawResponse = normalizeRawResponse(text);
            GeminiDecision decision = parseDecision(rawResponse);
            return AiReviewAiResult.builder()
                    .decision(AiReviewDecision.valueOf(decision.decision()))
                    .reason(decision.reason())
                    .confidence(decision.confidence())
                    .rawResponse(rawResponse)
                    .build();
        } catch (ApiException e) {
            throw e;
        } catch (ResourceAccessException e) {
            log.error("Gemini API 호출 중 타임아웃/연결 오류", e);
            throw new ApiException(ErrorCode.SERVER_ERROR, "Gemini API 응답이 지연되고 있습니다.");
        } catch (Exception e) {
            log.error("Gemini AI 검수 처리 실패", e);
            throw new ApiException(ErrorCode.SERVER_ERROR, "Gemini AI 검수 처리에 실패했습니다.");
        }
    }

    private GeminiRequest createRequest(String prompt, List<String> imageUrls) {
        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("text", prompt));

        if (imageUrls != null) {
            for (String imageUrl : imageUrls) {
                if (imageUrl == null || imageUrl.isBlank()) {
                    continue;
                }
                ImageData imageData = downloadImage(imageUrl);
                parts.add(Map.of("inline_data", Map.of(
                        "mime_type", imageData.mimeType(),
                        "data", Base64.getEncoder().encodeToString(imageData.bytes())
                )));
            }
        }

        return new GeminiRequest(List.of(new GeminiContent(parts)));
    }

    private ImageData downloadImage(String imageUrl) {
        URI uri = parseImageUri(imageUrl);
        validateImageUrl(uri);

        byte[] bytes = imageRestClient.get()
                .uri(uri)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new ApiException(ErrorCode.SERVER_ERROR, "AI 검수 이미지 다운로드에 실패했습니다.");
                })
                .body(byte[].class);

        if (bytes == null || bytes.length == 0) {
            throw new ApiException(ErrorCode.SERVER_ERROR, "AI 검수 이미지가 비어 있습니다.");
        }
        return new ImageData(detectMimeType(imageUrl), bytes);
    }

    private URI parseImageUri(String imageUrl) {
        try {
            return URI.create(imageUrl);
        } catch (IllegalArgumentException e) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void validateImageUrl(URI uri) {
        if (uri.getScheme() == null || uri.getHost() == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
        if (isAllowedS3Url(uri) || isAllowedLocalUrl(uri)) {
            return;
        }
        throw new ApiException(ErrorCode.INVALID_REQUEST);
    }

    private boolean isAllowedS3Url(URI uri) {
        if (!"https".equalsIgnoreCase(uri.getScheme()) || s3Bucket == null || s3Bucket.isBlank()) {
            return false;
        }
        String host = uri.getHost().toLowerCase();
        return host.startsWith(s3Bucket.toLowerCase() + ".s3.")
                && host.endsWith(".amazonaws.com");
    }

    private boolean isAllowedLocalUrl(URI uri) {
        if (!localImageUrlAllowed) {
            return false;
        }
        String host = uri.getHost();
        return "localhost".equalsIgnoreCase(host)
                || "127.0.0.1".equals(host)
                || "::1".equals(host);
    }

    private String detectMimeType(String imageUrl) {
        String lowerUrl = imageUrl.toLowerCase();
        if (lowerUrl.contains(".png")) {
            return MediaType.IMAGE_PNG_VALUE;
        }
        if (lowerUrl.contains(".webp")) {
            return "image/webp";
        }
        return MediaType.IMAGE_JPEG_VALUE;
    }

    private String extractText(GeminiResponse response) {
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            throw new ApiException(ErrorCode.SERVER_ERROR, "Gemini 응답이 비어 있습니다.");
        }

        GeminiContent content = response.candidates().getFirst().content();
        if (content == null || content.parts() == null || content.parts().isEmpty()) {
            throw new ApiException(ErrorCode.SERVER_ERROR, "Gemini 응답 본문이 비어 있습니다.");
        }

        Object text = content.parts().getFirst().get("text");
        if (!(text instanceof String value) || value.isBlank()) {
            throw new ApiException(ErrorCode.SERVER_ERROR, "Gemini 응답 텍스트가 비어 있습니다.");
        }
        return value;
    }

    private GeminiDecision parseDecision(String rawText) throws Exception {
        String json = stripMarkdownFence(rawText);
        GeminiDecision decision = objectMapper.readValue(json, GeminiDecision.class);
        validateDecision(decision);
        return decision;
    }

    private String normalizeRawResponse(String rawText) throws Exception {
        String json = stripMarkdownFence(rawText);
        return objectMapper.writeValueAsString(objectMapper.readTree(json));
    }

    private String stripMarkdownFence(String rawText) {
        String trimmed = rawText.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }
        return trimmed
                .replaceFirst("^```(?:json)?\\s*", "")
                .replaceFirst("\\s*```$", "")
                .trim();
    }

    private void validateDecision(GeminiDecision decision) {
        if (decision == null || decision.decision() == null || decision.reason() == null) {
            throw new ApiException(ErrorCode.SERVER_ERROR, "Gemini 응답 형식이 올바르지 않습니다.");
        }
        try {
            AiReviewDecision.valueOf(decision.decision());
        } catch (IllegalArgumentException e) {
            throw new ApiException(ErrorCode.SERVER_ERROR, "Gemini decision 값이 올바르지 않습니다.");
        }
        if (decision.confidence() < 0.0 || decision.confidence() > 1.0) {
            throw new ApiException(ErrorCode.SERVER_ERROR, "Gemini confidence 값이 올바르지 않습니다.");
        }
    }

    private record GeminiRequest(
            List<GeminiContent> contents
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeminiContent(
            List<Map<String, Object>> parts
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeminiResponse(
            List<GeminiCandidate> candidates
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeminiCandidate(
            GeminiContent content
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeminiDecision(
            String decision,
            String reason,
            double confidence
    ) {
    }

    private record ImageData(
            String mimeType,
            byte[] bytes
    ) {
    }
}
