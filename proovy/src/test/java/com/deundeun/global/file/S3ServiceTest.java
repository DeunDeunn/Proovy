package com.deundeun.global.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    @Mock
    private S3Client s3Client;

    @InjectMocks
    private S3Service s3Service;

    private void setBucketAndRegion() {
        ReflectionTestUtils.setField(s3Service, "region", "ap-northeast-2");
        ReflectionTestUtils.setField(s3Service, "bucket", "proovy-test-bucket");
    }

    @Test
    void upload_validImage_returnsPublicUrlWithUuidKey() {
        setBucketAndRegion();
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        MockMultipartFile file = new MockMultipartFile("file", "profile.png", "image/png", new byte[]{1, 2, 3});

        String url = s3Service.upload(file, FileCategory.PROFILE);

        assertThat(url).startsWith("https://proovy-test-bucket.s3.ap-northeast-2.amazonaws.com/profile/");
        assertThat(url).endsWith(".png");

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
        assertThat(requestCaptor.getValue().bucket()).isEqualTo("proovy-test-bucket");
        assertThat(requestCaptor.getValue().contentType()).isEqualTo("image/png");
        assertThat(requestCaptor.getValue().key()).startsWith("profile/").endsWith(".png");
    }

    @Test
    void upload_emptyFile_throwsAndDoesNotCallS3() {
        setBucketAndRegion();
        MockMultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> s3Service.upload(file, FileCategory.PROFILE))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.FILE_EMPTY);

        verify(s3Client, org.mockito.Mockito.never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void upload_disallowedContentType_throwsInvalidFileType() {
        setBucketAndRegion();
        // PROFILE 카테고리는 이미지만 허용하는데 실행 파일을 올리려는 상황
        MockMultipartFile file = new MockMultipartFile("file", "malware.exe", "application/x-msdownload", new byte[]{1});

        assertThatThrownBy(() -> s3Service.upload(file, FileCategory.PROFILE))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_FILE_TYPE);
    }

    @Test
    void upload_exceedsMaxSize_throwsFileTooLarge() {
        setBucketAndRegion();
        byte[] oversized = new byte[(int) FileCategory.PROFILE.getMaxFileSizeBytes() + 1];
        MockMultipartFile file = new MockMultipartFile("file", "big.png", "image/png", oversized);

        assertThatThrownBy(() -> s3Service.upload(file, FileCategory.PROFILE))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.FILE_TOO_LARGE);
    }

    @Test
    void upload_chatAllowsGifUnlikeOtherCategories() {
        setBucketAndRegion();
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        MockMultipartFile file = new MockMultipartFile("file", "meme.gif", "image/gif", new byte[]{1, 2});

        String url = s3Service.upload(file, FileCategory.CHAT);

        assertThat(url).contains("/chat/").endsWith(".gif");
    }

    @Test
    void upload_chatRejectsNonImageDocument() {
        setBucketAndRegion();
        // 사진만 허용하는 정책이라 CHAT도 pdf 같은 문서는 거부해야 한다
        MockMultipartFile file = new MockMultipartFile("file", "notes.pdf", "application/pdf", new byte[]{1, 2});

        assertThatThrownBy(() -> s3Service.upload(file, FileCategory.CHAT))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_FILE_TYPE);
    }

    @Test
    void upload_s3Failure_throwsFileUploadFailed() {
        setBucketAndRegion();
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().message("boom").build());
        MockMultipartFile file = new MockMultipartFile("file", "profile.png", "image/png", new byte[]{1});

        assertThatThrownBy(() -> s3Service.upload(file, FileCategory.PROFILE))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.FILE_UPLOAD_FAILED);
    }

    @Test
    void uploadAll_validImages_returnsUrlsInOrder() {
        setBucketAndRegion();
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        MockMultipartFile file1 = new MockMultipartFile("files", "a.png", "image/png", new byte[]{1});
        MockMultipartFile file2 = new MockMultipartFile("files", "b.jpg", "image/jpeg", new byte[]{2});

        List<String> urls = s3Service.uploadAll(List.of(file1, file2), FileCategory.CERTIFICATION);

        assertThat(urls).hasSize(2);
        assertThat(urls.get(0)).contains("/certification/").endsWith(".png");
        assertThat(urls.get(1)).contains("/certification/").endsWith(".jpg");
        verify(s3Client, times(2)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void uploadAll_oneFileFailsValidation_uploadsNothing() {
        setBucketAndRegion();
        MockMultipartFile valid = new MockMultipartFile("files", "a.png", "image/png", new byte[]{1});
        MockMultipartFile invalid = new MockMultipartFile("files", "b.exe", "application/x-msdownload", new byte[]{2});

        assertThatThrownBy(() -> s3Service.uploadAll(List.of(valid, invalid), FileCategory.CERTIFICATION))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_FILE_TYPE);

        // 뒤에 있는 파일이 검증에 실패하면, 앞에서 이미 유효했던 파일도 업로드가 아예 안 되어야 한다(부분 성공 없음)
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void uploadAll_emptyList_throwsFileEmpty() {
        setBucketAndRegion();

        assertThatThrownBy(() -> s3Service.uploadAll(List.of(), FileCategory.CERTIFICATION))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.FILE_EMPTY);
    }

    @Test
    void uploadAll_exceedsMaxBatchSize_throwsInvalidRequest() {
        setBucketAndRegion();
        List<MultipartFile> tooMany = java.util.stream.IntStream.range(0, 11)
                .mapToObj(i -> (MultipartFile) new MockMultipartFile("files", i + ".png", "image/png", new byte[]{1}))
                .toList();

        assertThatThrownBy(() -> s3Service.uploadAll(tooMany, FileCategory.CERTIFICATION))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);

        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }
}
