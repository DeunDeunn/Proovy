package com.deundeun.global.file;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * aws.credentials.access-key/secret-key가 설정돼 있으면(로컬 개발용) 그 값을 그대로 쓰고,
 * 없으면 DefaultCredentialsProvider로 폴백한다(운영은 IAM Role) — 코드 변경 없이
 * 환경별로 자격증명 방식이 자동으로 갈린다.
 */
@Configuration
public class S3Config {

    @Bean
    public S3Client s3Client(
            @Value("${aws.region}") String region,
            @Value("${aws.credentials.access-key:}") String accessKey,
            @Value("${aws.credentials.secret-key:}") String secretKey) {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(resolveCredentialsProvider(accessKey, secretKey))
                .build();
    }

    private AwsCredentialsProvider resolveCredentialsProvider(String accessKey, String secretKey) {
        if (accessKey.isBlank() || secretKey.isBlank()) {
            return DefaultCredentialsProvider.create();
        }
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
    }
}
