package com.deundeun.global.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;

class S3ConfigTest {

    private final S3Config s3Config = new S3Config();

    @Test
    void s3Client_bothCredentialsBlank_fallsBackToDefaultProviderWithoutThrowing() {
        // 둘 다 비어있으면 DefaultCredentialsProvider(운영 IAM Role 등)로 정상 폴백해야 한다
        S3Client client = s3Config.s3Client("ap-northeast-2", "", "");

        assertThat(client).isNotNull();
    }

    @Test
    void s3Client_bothCredentialsPresent_buildsWithStaticProviderWithoutThrowing() {
        S3Client client = s3Config.s3Client("ap-northeast-2", "AKIAEXAMPLE", "secretExample");

        assertThat(client).isNotNull();
    }

    @Test
    void s3Client_onlyAccessKeyPresent_throwsIllegalStateException() {
        assertThatThrownBy(() -> s3Config.s3Client("ap-northeast-2", "AKIAEXAMPLE", ""))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void s3Client_onlySecretKeyPresent_throwsIllegalStateException() {
        assertThatThrownBy(() -> s3Config.s3Client("ap-northeast-2", "", "secretExample"))
                .isInstanceOf(IllegalStateException.class);
    }
}
