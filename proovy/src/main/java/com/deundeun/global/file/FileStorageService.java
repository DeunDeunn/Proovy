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
 * <p>반대 방향은 자동으로 안 풀린다 — 업로드가 성공한 뒤 이어지는 DB 갱신이나 트랜잭션
 * 커밋 자체가 실패하면, DB에는 참조가 안 남는데 S3 객체는 그대로 남는 고아 파일이 된다.
 * {@code FileStorageService}는 호출자의 트랜잭션이 이후에 성공했는지 알 방법이 없으므로,
 * 이 보상 처리는 호출하는 도메인이 직접 해야 한다. {@link #delete}를 트랜잭션 동기화
 * 콜백에 등록해 롤백(커밋 실패 포함)됐을 때만 지우는 방식을 권장한다 — try/catch로는
 * updateThumbnail 실패만 잡히고, 메서드 리턴 이후 flush 시점의 커밋 실패는 못 잡기
 * 때문이다:</p>
 *
 * <pre>{@code
 * @Transactional
 * public CreateCertificationPostResponse create(Long userId, MultipartFile thumbnail, String contents) {
 *     CertificationPost post = CertificationPost.builder()
 *             .authorId(userId).contents(contents).thumbnailImage(null).build();
 *     certificationMapper.insert(post);                      // 1. DB 먼저 (아직 커밋 전)
 *
 *     String url = fileStorageService.upload(thumbnail, FileCategory.CERTIFICATION); // 2. 업로드는 나중
 *     TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
 *         @Override
 *         public void afterCompletion(int status) {
 *             if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
 *                 fileStorageService.delete(url); // 3. 이후 어디서 실패하든(갱신/커밋) 고아 파일 정리
 *             }
 *         }
 *     });
 *     certificationMapper.updateThumbnail(post.getId(), url); // 4. 확정 — 여기서 실패해도 위 콜백이 정리
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
     * <p>검증을 통과한 뒤에도 실제 업로드 도중 일부 파일만 성공하고 그다음 파일이 실패할 수
     * 있다(S3는 여러 객체를 하나로 묶어주지 않는다) — 이 경우 이미 성공한 파일들을 내부적으로
     * 다시 삭제하고 예외를 던지므로, 호출자 입장에서는 항상 "전부 성공 아니면 전부 실패"로
     * 취급해도 된다.</p>
     *
     * @throws com.deundeun.global.exception.ApiException FILE_EMPTY — files가 비어있거나, 그 안의 파일 중 하나가 비어있을 때
     * @throws com.deundeun.global.exception.ApiException FILE_TOO_LARGE — 파일 중 하나라도 category별 최대 용량을 초과했을 때
     * @throws com.deundeun.global.exception.ApiException INVALID_FILE_TYPE — 파일 중 하나라도 category가 허용하지 않는 타입일 때
     * @throws com.deundeun.global.exception.ApiException FILE_UPLOAD_FAILED — 스토리지 업로드 자체가 실패했을 때
     * @throws com.deundeun.global.exception.ApiException INVALID_REQUEST — 한 번에 업로드 가능한 개수를 초과했을 때
     */
    List<String> uploadAll(List<MultipartFile> files, FileCategory category);

    /**
     * {@link #upload}/{@link #uploadAll}이 반환한 URL의 객체를 스토리지에서 지운다.
     * 업로드 이후 도메인 쪽 처리(DB 갱신, 트랜잭션 커밋)가 실패했을 때 고아 파일을
     * 정리하는 보상 액션 용도다 — 삭제 자체가 실패해도 예외를 던지지 않고 best-effort로
     * 처리한다(이미 실패한 트랜잭션의 롤백 흐름을 이 호출이 다시 방해하면 안 되기 때문).
     */
    void delete(String url);
}
