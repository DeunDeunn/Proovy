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
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    // 실제 매직 넘버(파일 시그니처) — 검증 로직이 이제 이 바이트로만 타입을 판별한다
    private static final byte[] PNG_BYTES = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
    private static final byte[] JPEG_BYTES = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0, 0, 0};
    private static final byte[] GIF_BYTES = "GIF89a".getBytes();
    private static final byte[] WEBP_BYTES = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};
    private static final byte[] NOT_AN_IMAGE = "<html><script>alert(1)</script></html>".getBytes();
    private static final byte[] PDF_BYTES = "%PDF-1.7\n%".getBytes();
    private static final byte[] ZIP_BYTES = {'P', 'K', 0x03, 0x04, 0, 0, 0, 0};
    private static final byte[] OLE2_BYTES = {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1};
    private static final byte[] TEXT_BYTES = "hello world\nsecond line\ttabbed".getBytes();
    private static final byte[] BINARY_GARBAGE = {0x00, 0x01, 0x02, (byte) 0xFE, (byte) 0xFF, 0x00, 0x00};

    @Mock
    private S3Client s3Client;

    @InjectMocks
    private S3Service s3Service;

    private void setBucketAndRegion() {
        ReflectionTestUtils.setField(s3Service, "region", "ap-northeast-2");
        ReflectionTestUtils.setField(s3Service, "bucket", "proovy-test-bucket");
    }

    @Test
    void upload_validPngBytes_returnsPublicUrlWithUuidKey() {
        setBucketAndRegion();
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        MockMultipartFile file = new MockMultipartFile("file", "profile.png", "image/png", PNG_BYTES);

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

        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void upload_bytesDontMatchAnyKnownImageSignature_throwsInvalidFileType() {
        setBucketAndRegion();
        // 확장자/Content-Type 없이(또는 뭐라 선언하든) 실제 바이트 자체가 이미지가 아닌 경우
        MockMultipartFile file = new MockMultipartFile("file", "malware.exe", "application/x-msdownload", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> s3Service.upload(file, FileCategory.PROFILE))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_FILE_TYPE);
    }

    @Test
    void upload_declaredContentTypeLiesButActualBytesArentImage_throwsInvalidFileType() {
        setBucketAndRegion();
        // Content-Type을 image/png라고 속여도, 실제 바이트가 이미지가 아니면 거부돼야 한다
        MockMultipartFile file = new MockMultipartFile("file", "profile.png", "image/png", NOT_AN_IMAGE);

        assertThatThrownBy(() -> s3Service.upload(file, FileCategory.PROFILE))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_FILE_TYPE);

        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void upload_maliciousFilenameExtensionIsIgnored_keyUsesDetectedType() {
        setBucketAndRegion();
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        // 파일명은 .html이지만 실제 바이트는 진짜 PNG인 상황 — key/Content-Type이 파일명이 아니라
        // 실제 판별된 타입(png)을 따라야 한다
        MockMultipartFile file = new MockMultipartFile("file", "photo.html", "image/png", PNG_BYTES);

        String url = s3Service.upload(file, FileCategory.PROFILE);

        assertThat(url).endsWith(".png");
        assertThat(url).doesNotContain(".html");

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
        assertThat(requestCaptor.getValue().contentType()).isEqualTo("image/png");
        assertThat(requestCaptor.getValue().key()).endsWith(".png");
    }

    @Test
    void upload_exceedsMaxSize_throwsFileTooLarge() {
        setBucketAndRegion();
        byte[] oversized = new byte[(int) FileCategory.PROFILE.getMaxFileSizeBytes() + 1];
        System.arraycopy(PNG_BYTES, 0, oversized, 0, PNG_BYTES.length);
        MockMultipartFile file = new MockMultipartFile("file", "big.png", "image/png", oversized);

        assertThatThrownBy(() -> s3Service.upload(file, FileCategory.PROFILE))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.FILE_TOO_LARGE);
    }

    @Test
    void upload_jpegBytes_detectedAndUploaded() {
        setBucketAndRegion();
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", JPEG_BYTES);

        String url = s3Service.upload(file, FileCategory.CERTIFICATION);

        assertThat(url).contains("/certification/").endsWith(".jpg");
    }

    @Test
    void upload_webpBytes_detectedAndUploaded() {
        setBucketAndRegion();
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        MockMultipartFile file = new MockMultipartFile("file", "photo.webp", "image/webp", WEBP_BYTES);

        String url = s3Service.upload(file, FileCategory.CERTIFICATION);

        assertThat(url).contains("/certification/").endsWith(".webp");
    }

    @Test
    void upload_chatAllowsGifUnlikeOtherCategories() {
        setBucketAndRegion();
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        MockMultipartFile file = new MockMultipartFile("file", "meme.gif", "image/gif", GIF_BYTES);

        String url = s3Service.upload(file, FileCategory.CHAT);

        assertThat(url).contains("/chat/").endsWith(".gif");
    }

    @Test
    void upload_profileRejectsGifEvenThoughBytesAreValidGif() {
        setBucketAndRegion();
        // GIF 자체는 진짜 이미지지만, PROFILE 카테고리는 gif를 허용 목록에 안 넣어뒀다
        MockMultipartFile file = new MockMultipartFile("file", "meme.gif", "image/gif", GIF_BYTES);

        assertThatThrownBy(() -> s3Service.upload(file, FileCategory.PROFILE))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_FILE_TYPE);
    }

    @Test
    void upload_s3Failure_throwsFileUploadFailed() {
        setBucketAndRegion();
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().message("boom").build());
        MockMultipartFile file = new MockMultipartFile("file", "profile.png", "image/png", PNG_BYTES);

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
        MockMultipartFile file1 = new MockMultipartFile("files", "a.png", "image/png", PNG_BYTES);
        MockMultipartFile file2 = new MockMultipartFile("files", "b.jpg", "image/jpeg", JPEG_BYTES);

        List<String> urls = s3Service.uploadAll(List.of(file1, file2), FileCategory.CERTIFICATION);

        assertThat(urls).hasSize(2);
        assertThat(urls.get(0)).contains("/certification/").endsWith(".png");
        assertThat(urls.get(1)).contains("/certification/").endsWith(".jpg");
        verify(s3Client, times(2)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void uploadAll_secondFileFailsMidUpload_cleansUpAlreadyUploadedFiles() {
        setBucketAndRegion();
        // 1번째 파일은 실제 PUT까지 성공하고, 2번째 파일에서 (검증 통과 후) S3 자체 오류가 나는 상황
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build())
                .thenThrow(S3Exception.builder().message("boom").build());
        MockMultipartFile file1 = new MockMultipartFile("files", "a.png", "image/png", PNG_BYTES);
        MockMultipartFile file2 = new MockMultipartFile("files", "b.png", "image/png", PNG_BYTES);

        assertThatThrownBy(() -> s3Service.uploadAll(List.of(file1, file2), FileCategory.CERTIFICATION))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.FILE_UPLOAD_FAILED);

        ArgumentCaptor<PutObjectRequest> putCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client, times(2)).putObject(putCaptor.capture(), any(RequestBody.class));
        String firstUploadedKey = putCaptor.getAllValues().get(0).key();

        // 이미 성공한 1번째 파일은 실패 이후 자동으로 삭제되어야 orphan이 안 남는다
        ArgumentCaptor<DeleteObjectRequest> deleteCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client, times(1)).deleteObject(deleteCaptor.capture());
        assertThat(deleteCaptor.getValue().key()).isEqualTo(firstUploadedKey);
    }

    @Test
    void uploadAll_oneFileFailsValidation_uploadsNothing() {
        setBucketAndRegion();
        MockMultipartFile valid = new MockMultipartFile("files", "a.png", "image/png", PNG_BYTES);
        MockMultipartFile invalid = new MockMultipartFile("files", "b.exe", "application/x-msdownload", new byte[]{1, 2, 3});

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
                .mapToObj(i -> (MultipartFile) new MockMultipartFile("files", i + ".png", "image/png", PNG_BYTES))
                .toList();

        assertThatThrownBy(() -> s3Service.uploadAll(tooMany, FileCategory.CERTIFICATION))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);

        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void delete_validUrl_deletesExtractedKey() {
        setBucketAndRegion();
        String url = "https://proovy-test-bucket.s3.ap-northeast-2.amazonaws.com/profile/some-uuid.png";

        s3Service.delete(url);

        ArgumentCaptor<DeleteObjectRequest> requestCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(requestCaptor.capture());
        assertThat(requestCaptor.getValue().bucket()).isEqualTo("proovy-test-bucket");
        assertThat(requestCaptor.getValue().key()).isEqualTo("profile/some-uuid.png");
    }

    @Test
    void delete_s3Failure_isSwallowedAndDoesNotThrow() {
        setBucketAndRegion();
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(S3Exception.builder().message("boom").build());
        String url = "https://proovy-test-bucket.s3.ap-northeast-2.amazonaws.com/profile/some-uuid.png";

        // 보상(고아 파일 정리) 액션은 실패해도 이미 진행 중인 롤백 흐름을 막으면 안 되므로 예외를 던지지 않는다
        s3Service.delete(url);

        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void delete_urlWithoutAmazonawsMarker_throwsInvalidRequest() {
        setBucketAndRegion();

        assertThatThrownBy(() -> s3Service.delete("not-a-valid-url"))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);

        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void upload_pdfBytes_detectedAndUploaded() {
        setBucketAndRegion();
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", PDF_BYTES);

        String url = s3Service.upload(file, FileCategory.CHAT);

        assertThat(url).contains("/chat/").endsWith(".pdf");
    }

    @Test
    void upload_zipBytesWithDocxDeclaredType_resolvesToDocx() {
        setBucketAndRegion();
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        MockMultipartFile file = new MockMultipartFile("file", "report.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", ZIP_BYTES);

        String url = s3Service.upload(file, FileCategory.CHAT);

        assertThat(url).endsWith(".docx");
    }

    @Test
    void upload_zipBytesWithXlsxDeclaredType_resolvesToXlsx() {
        setBucketAndRegion();
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        MockMultipartFile file = new MockMultipartFile("file", "sheet.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ZIP_BYTES);

        String url = s3Service.upload(file, FileCategory.CHAT);

        assertThat(url).endsWith(".xlsx");
    }

    @Test
    void upload_zipBytesWithPlainZipDeclaredType_resolvesToZip() {
        setBucketAndRegion();
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        MockMultipartFile file = new MockMultipartFile("file", "archive.zip", "application/zip", ZIP_BYTES);

        String url = s3Service.upload(file, FileCategory.CHAT);

        assertThat(url).endsWith(".zip");
    }

    @Test
    void upload_zipBytesWithDeclaredTypeOutsideFamily_throwsInvalidFileType() {
        setBucketAndRegion();
        // 실제 바이트는 zip 계열인데, 그 그룹에 속하지 않는 타입을 선언한 경우 — 거부돼야 함
        MockMultipartFile file = new MockMultipartFile("file", "fake.png", "image/png", ZIP_BYTES);

        assertThatThrownBy(() -> s3Service.upload(file, FileCategory.CHAT))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_FILE_TYPE);
    }

    @Test
    void upload_ole2BytesWithDocDeclaredType_resolvesToDoc() {
        setBucketAndRegion();
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        MockMultipartFile file = new MockMultipartFile("file", "legacy.doc", "application/msword", OLE2_BYTES);

        String url = s3Service.upload(file, FileCategory.CHAT);

        assertThat(url).endsWith(".doc");
    }

    @Test
    void upload_ole2BytesWithXlsDeclaredType_resolvesToXls() {
        setBucketAndRegion();
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        MockMultipartFile file = new MockMultipartFile("file", "legacy.xls", "application/vnd.ms-excel", OLE2_BYTES);

        String url = s3Service.upload(file, FileCategory.CHAT);

        assertThat(url).endsWith(".xls");
    }

    @Test
    void upload_ole2BytesWithDeclaredTypeOutsideFamily_throwsInvalidFileType() {
        setBucketAndRegion();
        MockMultipartFile file = new MockMultipartFile("file", "fake.pdf", "application/pdf", OLE2_BYTES);

        assertThatThrownBy(() -> s3Service.upload(file, FileCategory.CHAT))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_FILE_TYPE);
    }

    @Test
    void upload_plainTextBytes_detectedAsTextPlain() {
        setBucketAndRegion();
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        MockMultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain", TEXT_BYTES);

        String url = s3Service.upload(file, FileCategory.CHAT);

        assertThat(url).endsWith(".txt");
    }

    @Test
    void upload_binaryGarbageMatchingNoKnownFormat_throwsInvalidFileType() {
        setBucketAndRegion();
        // 어떤 시그니처와도 안 맞고, NUL/제어문자가 섞여 있어 텍스트로도 통과하면 안 됨
        MockMultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain", BINARY_GARBAGE);

        assertThatThrownBy(() -> s3Service.upload(file, FileCategory.CHAT))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_FILE_TYPE);
    }

    @Test
    void upload_profileRejectsPdfEvenThoughBytesAreValidPdf() {
        setBucketAndRegion();
        // PDF 자체는 진짜 PDF지만, PROFILE 카테고리는 문서 타입을 허용하지 않음
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", PDF_BYTES);

        assertThatThrownBy(() -> s3Service.upload(file, FileCategory.PROFILE))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_FILE_TYPE);
    }
}
