package com.deundeun.ai.client;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

import com.deundeun.ai.config.GeminiProperties;
import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.List;

@DisplayName("GeminiAiReviewClient")
class GeminiAiReviewClientTest {

    @Test
    @DisplayName("S3 이미지 URL은 허용한다")
    void validateImageUrl_s3Url_allows() {
        GeminiAiReviewClient client = client(new MockEnvironment());
        URI uri = URI.create("https://deundeun-s3-2026.s3.ap-northeast-2.amazonaws.com/certifications/test.png");

        assertThatCode(() -> ReflectionTestUtils.invokeMethod(client, "validateImageUrl", uri))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("local 프로필에서는 로컬 이미지 URL을 허용한다")
    void validateImageUrl_localProfileLocalhost_allows() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("local");
        GeminiAiReviewClient client = client(environment);
        URI uri = URI.create("http://127.0.0.1:8099/test.png");

        assertThatCode(() -> ReflectionTestUtils.invokeMethod(client, "validateImageUrl", uri))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("local 프로필이 아니면 로컬 이미지 URL을 거부한다")
    void validateImageUrl_nonLocalProfileLocalhost_rejects() {
        GeminiAiReviewClient client = client(new MockEnvironment());
        URI uri = URI.create("http://127.0.0.1:8099/test.png");

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(client, "validateImageUrl", uri))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.AI_REVIEW_IMAGE_INVALID_URL);
    }

    @Test
    @DisplayName("S3나 로컬 테스트 URL이 아닌 이미지는 거부한다")
    void validateImageUrl_externalUrl_rejects() {
        GeminiAiReviewClient client = client(new MockEnvironment());
        URI uri = URI.create("https://example.com/test.png");

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(client, "validateImageUrl", uri))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.AI_REVIEW_IMAGE_INVALID_URL);
    }

    @Test
    @DisplayName("rejects more than 4 images before download")
    void createRequest_tooManyImages_rejectsBeforeDownload() {
        GeminiAiReviewClient client = client(new MockEnvironment());
        List<String> imageUrls = List.of(
                "https://deundeun-s3-2026.s3.ap-northeast-2.amazonaws.com/certifications/1.jpg",
                "https://deundeun-s3-2026.s3.ap-northeast-2.amazonaws.com/certifications/2.jpg",
                "https://deundeun-s3-2026.s3.ap-northeast-2.amazonaws.com/certifications/3.jpg",
                "https://deundeun-s3-2026.s3.ap-northeast-2.amazonaws.com/certifications/4.jpg",
                "https://deundeun-s3-2026.s3.ap-northeast-2.amazonaws.com/certifications/5.jpg"
        );

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(client, "createRequest", "prompt", imageUrls))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.AI_REVIEW_IMAGE_COUNT_EXCEEDED);
    }

    @Test
    @DisplayName("reads image stream up to the allowed limit")
    void readImageBytes_limitExactSize_allows() {
        GeminiAiReviewClient client = client(new MockEnvironment());
        byte[] source = new byte[]{1, 2, 3};

        byte[] result = ReflectionTestUtils.invokeMethod(
                client,
                "readImageBytes",
                new ByteArrayInputStream(source),
                3L
        );

        assertThat(result).containsExactly(source);
    }

    @Test
    @DisplayName("rejects image stream when the limit is exceeded")
    void readImageBytes_limitExceeded_rejects() {
        GeminiAiReviewClient client = client(new MockEnvironment());
        byte[] source = new byte[]{1, 2, 3, 4};

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
                client,
                "readImageBytes",
                new ByteArrayInputStream(source),
                3L
        )).isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.AI_REVIEW_IMAGE_TOO_LARGE);
    }

    private GeminiAiReviewClient client(MockEnvironment environment) {
        GeminiProperties properties = new GeminiProperties(
                new GeminiProperties.Api("test-api-key", null),
                "gemini-2.5-flash-lite"
        );
        return new GeminiAiReviewClient(properties, environment, "deundeun-s3-2026");
    }
}
