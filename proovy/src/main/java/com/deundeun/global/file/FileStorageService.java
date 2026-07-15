package com.deundeun.global.file;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 각 도메인이 파일을 스토리지(S3)에 업로드할 때 쓰는 계약. 도메인은 어떤 클라우드 스토리지를
 * 쓰는지 알 필요 없이 이 인터페이스만 보고 호출하면 된다.
 *
 * <p>업로드 자체는 롤백이 안 되는 부수효과라, 도메인 쪽 DB 쓰기와 안전하게 엮으려면
 * 호출 순서가 중요하다 — DB row를 먼저(아직 커밋 전 상태로) 만들고 이 메서드를 나중에
 * 호출하는 걸 권장한다. 그래야 업로드가 실패했을 때 도메인 쪽 트랜잭션도 자동으로
 * 롤백되어, 업로드는 안 됐는데 DB엔 남아있는 상태를 피할 수 있다.</p>
 *
 * <pre>{@code
 * @Transactional
 * public CreateCertificationPostResponse create(Long userId, MultipartFile thumbnail, String contents) {
 *     CertificationPost post = CertificationPost.builder()
 *             .authorId(userId).contents(contents).thumbnailImage(null).build();
 *     certificationMapper.insert(post);                      // 1. DB 먼저 (아직 커밋 전)
 *
 *     String url = fileStorageService.upload(thumbnail, FileCategory.CERTIFICATION); // 2. 업로드는 나중
 *     certificationMapper.updateThumbnail(post.getId(), url); // 3. 확정
 *
 *     return CreateCertificationPostResponse.from(post);
 * }
 * }</pre>
 */
public interface FileStorageService {

    /**
     * 파일 하나를 업로드하고 접근 가능한 URL을 반환한다.
     *
     * @throws com.deundeun.global.exception.ApiException FILE_EMPTY — 파일이 비어있을 때
     * @throws com.deundeun.global.exception.ApiException FILE_TOO_LARGE — category별 최대 용량을 초과했을 때
     * @throws com.deundeun.global.exception.ApiException INVALID_FILE_TYPE — category가 허용하지 않는 타입일 때
     * @throws com.deundeun.global.exception.ApiException FILE_UPLOAD_FAILED — 스토리지 업로드 자체가 실패했을 때
     */
    String upload(MultipartFile file, FileCategory category);

    /**
     * 여러 파일을 한 번에 업로드하고 URL 목록을 반환한다(입력 순서 그대로). 업로드를 실제로
     * 시작하기 전에 전체 파일을 먼저 검증해서, 하나라도 실패하면 아무것도 업로드하지 않는다.
     * 각 파일에 대한 검증/업로드 자체는 {@link #upload}와 동일한 규칙을 그대로 따른다.
     *
     * @throws com.deundeun.global.exception.ApiException FILE_EMPTY — files가 비어있거나, 그 안의 파일 중 하나가 비어있을 때
     * @throws com.deundeun.global.exception.ApiException FILE_TOO_LARGE — 파일 중 하나라도 category별 최대 용량을 초과했을 때
     * @throws com.deundeun.global.exception.ApiException INVALID_FILE_TYPE — 파일 중 하나라도 category가 허용하지 않는 타입일 때
     * @throws com.deundeun.global.exception.ApiException FILE_UPLOAD_FAILED — 스토리지 업로드 자체가 실패했을 때
     * @throws com.deundeun.global.exception.ApiException INVALID_REQUEST — 한 번에 업로드 가능한 개수를 초과했을 때
     */
    List<String> uploadAll(List<MultipartFile> files, FileCategory category);
}
