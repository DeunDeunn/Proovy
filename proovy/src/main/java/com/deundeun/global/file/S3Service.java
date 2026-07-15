package com.deundeun.global.file;

import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class S3Service implements FileStorageService {

    private static final int MAX_BATCH_SIZE = 10;

    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] GIF_SIGNATURE = {'G', 'I', 'F', '8'}; // GIF87a/GIF89a 공통 접두사
    private static final int HEADER_PROBE_BYTES = 12; // WEBP(RIFF....WEBP) 판별에 필요한 최대 바이트 수

    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
            "image/png", ".png",
            "image/jpeg", ".jpg",
            "image/gif", ".gif",
            "image/webp", ".webp"
    );

    private final S3Client s3Client;

    @Value("${aws.region}")
    private String region;

    @Value("${aws.s3.bucket}")
    private String bucket;

    /**
     * 파일을 검증(용량/타입) 후 S3에 업로드하고, 접근 가능한 URL을 반환한다.
     * 원본 파일명은 key에 그대로 쓰지 않고 UUID로 대체해 경로 조작/충돌을 막는다.
     */
    @Override
    public String upload(MultipartFile file, FileCategory category) {
        String contentType = validate(file, category);
        return uploadValidated(file, category, contentType);
    }

    /**
     * 여러 파일을 한 번에 업로드한다. 하나라도 검증(용량/타입)에 실패하면 아무것도
     * 업로드하지 않고 즉시 예외를 던진다 — 일부만 올라간 채 실패하는 상황을 피하기 위해
     * 실제 업로드 전에 전체 파일을 먼저 검증한다.
     */
    @Override
    public List<String> uploadAll(List<MultipartFile> files, FileCategory category) {
        if (files == null || files.isEmpty()) {
            throw new ApiException(ErrorCode.FILE_EMPTY);
        }
        if (files.size() > MAX_BATCH_SIZE) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
        List<String> contentTypes = files.stream().map(file -> validate(file, category)).toList();
        return IntStream.range(0, files.size())
                .mapToObj(i -> uploadValidated(files.get(i), category, contentTypes.get(i)))
                .toList();
    }

    private String uploadValidated(MultipartFile file, FileCategory category, String contentType) {
        String key = buildKey(category, contentType);
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(contentType)
                            .contentLength(file.getSize())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
        } catch (IOException | SdkException e) {
            throw new ApiException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        return "https://%s.s3.%s.amazonaws.com/%s".formatted(bucket, region, key);
    }

    /**
     * 파일 존재 여부, 최대 크기 초과 여부를 확인하고, 클라이언트가 보낸 Content-Type/파일명은
     * 신뢰하지 않고 실제 바이트(매직 넘버)로 진짜 타입을 판별해서 검증한다.
     *
     * @return 판별된(서버가 결정한) 콘텐츠 타입 — key 확장자와 S3 Content-Type 메타데이터에 그대로 쓰인다.
     */
    private String validate(MultipartFile file, FileCategory category) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorCode.FILE_EMPTY);
        }
        if (file.getSize() > category.getMaxFileSizeBytes()) {
            throw new ApiException(ErrorCode.FILE_TOO_LARGE);
        }
        String detectedContentType = detectContentType(file);
        if (detectedContentType == null || !category.isAllowedContentType(detectedContentType)) {
            throw new ApiException(ErrorCode.INVALID_FILE_TYPE);
        }
        return detectedContentType;
    }

    /**
     * 파일 앞부분 바이트(매직 넘버)를 읽어 실제 이미지 형식을 판별한다. 클라이언트가 선언한
     * Content-Type이나 파일명 확장자는 위조 가능해서 신뢰하지 않는다.
     */
    private String detectContentType(MultipartFile file) {
        byte[] header = readHeader(file);
        if (startsWith(header, PNG_SIGNATURE)) {
            return "image/png";
        }
        if (startsWith(header, JPEG_SIGNATURE)) {
            return "image/jpeg";
        }
        if (startsWith(header, GIF_SIGNATURE)) {
            return "image/gif";
        }
        if (isWebp(header)) {
            return "image/webp";
        }
        return null;
    }

    private byte[] readHeader(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            return is.readNBytes(HEADER_PROBE_BYTES);
        } catch (IOException e) {
            throw new ApiException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    private boolean startsWith(byte[] data, byte[] signature) {
        if (data.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (data[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean isWebp(byte[] header) {
        // RIFF <4바이트 파일 크기> WEBP
        return header.length >= HEADER_PROBE_BYTES
                && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
    }

    private String buildKey(FileCategory category, String contentType) {
        return "%s/%s%s".formatted(category.getDirectory(), UUID.randomUUID(),
                EXTENSION_BY_CONTENT_TYPE.getOrDefault(contentType, ""));
    }
}
