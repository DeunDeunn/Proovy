package com.deundeun.global.file;

import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service implements FileStorageService {

    private static final int MAX_BATCH_SIZE = 10;

    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] GIF_SIGNATURE = {'G', 'I', 'F', '8'}; // GIF87a/GIF89a 공통 접두사
    private static final byte[] PDF_SIGNATURE = {'%', 'P', 'D', 'F', '-'};
    // ZIP 로컬 파일 헤더 — DOCX/XLSX도 내부적으로 ZIP 컨테이너라 이 시그니처를 그대로 공유한다
    private static final byte[] ZIP_SIGNATURE = {'P', 'K', 0x03, 0x04};
    // OLE2 복합 파일 시그니처 — 옛날 포맷 DOC/XLS가 같은 컨테이너 포맷이라 이것도 공유한다
    private static final byte[] OLE2_SIGNATURE = {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1};
    private static final int HEADER_PROBE_BYTES = 12; // WEBP(RIFF....WEBP) 판별에 필요한 최대 바이트 수
    private static final int TEXT_PROBE_BYTES = 1024; // TXT 판별용 샘플 크기 — 매직 넘버가 없어 느슨하게만 확인

    /**
     * ZIP/OLE2는 매직 넘버만으로 내부 포맷(zip vs docx vs xlsx, doc vs xls)까지는 구분이
     * 안 된다 — 컨테이너 시그니처가 같기 때문이다. 그래서 바이트로는 "이 그룹에 속하는
     * 컨테이너다"라는 것까지만 확인하고, 그 그룹 안에서 정확히 뭔지는 클라이언트가 보낸
     * Content-Type을 신뢰한다 — 그룹 밖으로 위장하는 건 여전히 막히고, 그룹 안에서의
     * 위장(docx를 xlsx라고 하는 등)만 허용하는 절충이다.
     */
    private static final Set<String> ZIP_FAMILY = Set.of(
            "application/zip",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );
    private static final Set<String> OLE2_FAMILY = Set.of(
            "application/msword",
            "application/vnd.ms-excel"
    );

    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.ofEntries(
            Map.entry("image/png", ".png"),
            Map.entry("image/jpeg", ".jpg"),
            Map.entry("image/gif", ".gif"),
            Map.entry("image/webp", ".webp"),
            Map.entry("application/pdf", ".pdf"),
            Map.entry("text/plain", ".txt"),
            Map.entry("application/msword", ".doc"),
            Map.entry("application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx"),
            Map.entry("application/vnd.ms-excel", ".xls"),
            Map.entry("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx"),
            Map.entry("application/zip", ".zip")
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
     * 업로드하지 않고 즉시 예외를 던진다 — 실제 업로드 전에 전체 파일을 먼저 검증해서,
     * "검증에 걸리는 파일이 있어서" 실패하는 경우는 아무것도 안 올라간 상태로 막는다.
     *
     * <p>다만 검증을 통과한 뒤 실제 업로드(S3 PUT) 도중 일부만 성공하고 그다음 파일이
     * 네트워크 오류 등으로 실패할 수는 있다 — S3는 여러 객체를 하나의 트랜잭션으로
     * 묶어주지 않기 때문이다. 그 경우 이미 성공한 파일들을 바로 삭제해서, 호출자
     * 입장에서는 "전부 성공 아니면 전부 실패"에 가깝게 동작하도록 보정한다.</p>
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

        List<String> uploadedUrls = new ArrayList<>();
        try {
            for (int i = 0; i < files.size(); i++) {
                uploadedUrls.add(uploadValidated(files.get(i), category, contentTypes.get(i)));
            }
        } catch (ApiException e) {
            uploadedUrls.forEach(this::delete);
            throw e;
        }
        return uploadedUrls;
    }

    /**
     * 고아 파일 정리용 best-effort 삭제 — 이미 실패한 도메인 트랜잭션의 롤백 흐름을
     * 방해하지 않도록, 삭제가 실패해도 예외를 던지지 않고 로그만 남긴다.
     */
    @Override
    public void delete(String url) {
        String key = extractKey(url);
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (SdkException e) {
            log.warn("[S3] 고아 파일 삭제 실패, 수동 정리 필요: bucket={}, key={}", bucket, key, e);
        }
    }

    private String extractKey(String url) {
        String marker = ".amazonaws.com/";
        int idx = url.indexOf(marker);
        if (idx < 0) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
        return url.substring(idx + marker.length());
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
                    RequestBody.fromContentProvider(
                            ContentStreamProvider.fromInputStreamSupplier(() -> openStream(file)),
                            file.getSize(),
                            contentType)
            );
        } catch (SdkException e) {
            throw new ApiException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        return "https://%s.s3.%s.amazonaws.com/%s".formatted(bucket, region, key);
    }

    /**
     * 재시도 시마다 {@link ContentStreamProvider#fromInputStreamSupplier}가 다시 호출해서
     * 처음부터 다시 읽을 수 있도록, 매번 새 스트림을 여는 공급자. 이전 시도에서 열어둔
     * 스트림을 닫는 것도 {@code fromInputStreamSupplier}가 알아서 해주므로 여기서
     * 별도로 닫지 않는다.
     */
    private InputStream openStream(MultipartFile file) {
        try {
            return file.getInputStream();
        } catch (IOException e) {
            throw new ApiException(ErrorCode.FILE_UPLOAD_FAILED);
        }
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
        if (!category.isAllowedContentType(detectedContentType)) {
            throw new ApiException(ErrorCode.INVALID_FILE_TYPE);
        }
        return detectedContentType;
    }

    /**
     * 파일 앞부분 바이트(매직 넘버)를 읽어 실제 파일 형식을 판별한다. 클라이언트가 선언한
     * Content-Type이나 파일명 확장자는 위조 가능해서 신뢰하지 않는 게 원칙이지만, ZIP/OLE2
     * 계열은 내부 포맷까지 바이트만으로 구분이 안 돼서 {@link #resolveAmbiguous}로 넘어간다.
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
        if (startsWith(header, PDF_SIGNATURE)) {
            return "application/pdf";
        }
        if (startsWith(header, ZIP_SIGNATURE)) {
            return resolveAmbiguous(file, ZIP_FAMILY);
        }
        if (startsWith(header, OLE2_SIGNATURE)) {
            return resolveAmbiguous(file, OLE2_FAMILY);
        }
        if (looksLikeText(file)) {
            return "text/plain";
        }
        return null;
    }

    /**
     * 바이트로는 "이 그룹(zip 계열/OLE2 계열)에 속한다"는 것만 확인된 상태에서, 그룹 안의
     * 정확한 타입은 클라이언트가 보낸 Content-Type을 신뢰해서 고른다. 선언된 타입이 그
     * 그룹에 속하지 않으면(예: 그룹은 확인됐는데 엉뚱한 타입을 선언한 경우) 거부한다.
     */
    private String resolveAmbiguous(MultipartFile file, Set<String> family) {
        String declared = file.getContentType();
        return family.contains(declared) ? declared : null;
    }

    /**
     * TXT는 고정된 매직 넘버가 없어 정확한 판별이 불가능하다 — 대신 샘플 바이트에
     * NUL이나 그 외 제어 문자가 없는지(탭/개행/CR 제외)만 느슨하게 확인해서, 명백한
     * 바이너리 파일이 텍스트로 위장하는 것 정도만 걸러낸다.
     */
    private boolean looksLikeText(MultipartFile file) {
        byte[] sample = readSample(file, TEXT_PROBE_BYTES);
        if (sample.length == 0) {
            return false;
        }
        for (byte b : sample) {
            int unsigned = b & 0xFF;
            boolean isDisallowedControl = unsigned < 0x20 && unsigned != '\t' && unsigned != '\n' && unsigned != '\r';
            if (isDisallowedControl || unsigned == 0x7F) {
                return false;
            }
        }
        return true;
    }

    private byte[] readHeader(MultipartFile file) {
        return readSample(file, HEADER_PROBE_BYTES);
    }

    private byte[] readSample(MultipartFile file, int maxBytes) {
        try (InputStream is = file.getInputStream()) {
            return is.readNBytes(maxBytes);
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
