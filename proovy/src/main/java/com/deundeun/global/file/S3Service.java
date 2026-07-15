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
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service implements FileStorageService {

    private static final int MAX_BATCH_SIZE = 10;

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
        validate(file, category);
        return uploadValidated(file, category);
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
        files.forEach(file -> validate(file, category));
        return files.stream().map(file -> uploadValidated(file, category)).toList();
    }

    private String uploadValidated(MultipartFile file, FileCategory category) {
        String key = buildKey(category, file.getOriginalFilename());
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(file.getContentType())
                            .contentLength(file.getSize())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
        } catch (IOException | SdkException e) {
            throw new ApiException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        return "https://%s.s3.%s.amazonaws.com/%s".formatted(bucket, region, key);
    }

    // 기본 검증 로직(파일 존재 여부, 최대 크기 초과 여부, 지원하지 않는 타입 여부)
    private void validate(MultipartFile file, FileCategory category) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorCode.FILE_EMPTY);
        }
        if (file.getSize() > category.getMaxFileSizeBytes()) {
            throw new ApiException(ErrorCode.FILE_TOO_LARGE);
        }
        if (!category.isAllowedContentType(file.getContentType())) {
            throw new ApiException(ErrorCode.INVALID_FILE_TYPE);
        }
    }

    private String buildKey(FileCategory category, String originalFilename) {
        return "%s/%s%s".formatted(category.getDirectory(), UUID.randomUUID(), extractExtension(originalFilename));
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int dotIndex = originalFilename.lastIndexOf('.');
        return dotIndex >= 0 ? originalFilename.substring(dotIndex) : "";
    }
}
