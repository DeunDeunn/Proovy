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
        boolean accessKeyBlank = accessKey.isBlank();
        boolean secretKeyBlank = secretKey.isBlank();

        if (accessKeyBlank && secretKeyBlank) {
            return DefaultCredentialsProvider.create();
        }
        if (accessKeyBlank || secretKeyBlank) {
            // 둘 중 하나만 설정된 경우는 오타/설정 누락일 가능성이 높다. 조용히 DefaultCredentialsProvider로
            // 넘어가면 한참 뒤 첫 S3 호출 시점에야 알 수 없는 자격증명 오류로 터지니, 부팅 시점에 바로 막는다.
            throw new IllegalStateException(
                    "aws.credentials.access-key와 aws.credentials.secret-key는 둘 다 설정하거나 둘 다 비워둬야 합니다. "
                            + "하나만 설정되어 있습니다.");
        }
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
    }
}
