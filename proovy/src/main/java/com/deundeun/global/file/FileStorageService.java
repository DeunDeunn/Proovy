package com.deundeun.global.file;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 각 도메인이 파일을 스토리지(S3)에 업로드할 때 쓰는 계약. 도메인은 어떤 클라우드 스토리지를
 * 쓰는지 알 필요 없이 이 인터페이스만 보고 호출하면 된다.
 *
 * <p>업로드 자체는 롤백이 안 되는 부수효과라, 도메인 쪽 DB 쓰기와 안전하게 엮으려면
 * 호출 순서가 중요하다 — 업로드(또는 최소한 {@link #reserveUrl})를 먼저 하고, 그 결과
 * url을 채운 DB row를 만든 다음, 실제 업로드는 맨 마지막에 하는 걸 권장한다. 이렇게 하면
 * "되돌릴 수 없는 부수효과(S3 쓰기)"가 항상 트랜잭션의 마지막 동작이 되어, 그 앞의 DB
 * 쓰기가 실패하는 경우는 업로드 자체가 아예 시도되지 않거나(아직 실행 전) 자동으로
 * 롤백되므로 고아 파일이 생기지 않는다.</p>
 *
 * <p>이때 DB에 저장할 url을 미리 알아야 하는데, {@link #reserveUrl}로 실제 업로드 없이
 * (검증만 하고) url을 먼저 발급받을 수 있다. 이후 {@link #uploadTo}로 그 url이 가리키는
 * 정확히 같은 위치에 실제 업로드를 수행한다.</p>
 *
 * <p>그래도 딱 한 군데, {@code uploadTo}가 성공한 뒤 메서드가 리턴되고 나서 트랜잭션
 * 프록시가 실행하는 실제 COMMIT 자체가 실패하는 아주 좁은 구간은 자동으로 안 풀린다 —
 * 이 경우 DB에는 참조가 안 남는데 S3 객체는 그대로 남는 고아 파일이 된다.
 * {@code FileStorageService}는 호출자의 트랜잭션이 이후에 성공했는지 알 방법이 없으므로,
 * 이 보상 처리는 호출하는 도메인이 직접 해야 한다. {@link #delete}를 트랜잭션 동기화
 * 콜백에 등록해 {@code STATUS_ROLLED_BACK}(커밋 자체가 실패해서 롤백으로 처리된 경우
 * 포함)일 때만 지우는 방식을 권장한다:</p>
 *
 * <p>{@code STATUS_UNKNOWN}(커밋이 성공했는지조차 트랜잭션 매니저가 확정하지 못하는
 * 드문 상황)은 일부러 삭제 대상에서 뺐다 — 실제로는 커밋이 성공했는데 이 콜백만
 * {@code STATUS_UNKNOWN}으로 불렸을 경우, 여기서 지워버리면 DB row는 파일을 참조하는
 * 채로 살아있는데 실제 파일만 사라지는 "깨진 링크"가 생겨 orphan 파일보다 더 나쁜
 * 상태가 된다. 이 경우는 즉시 자동 삭제 대신 별도의 정합성 점검(예: S3 객체와 DB
 * 참조를 주기적으로 대조하는 배치)으로 다뤄야 한다:</p>
 *
 * <pre>{@code
 * @Transactional
 * public CreateCertificationPostResponse create(Long userId, MultipartFile thumbnail, String contents) {
 *     String url = fileStorageService.reserveUrl(thumbnail, FileCategory.CERTIFICATION); // 1. url만 미리 발급 (업로드 안 함)
 *
 *     CertificationPost post = CertificationPost.builder()
 *             .authorId(userId).contents(contents).thumbnailImage(url).build();
 *     certificationMapper.insert(post); // 2. url을 채운 채로 insert (아직 커밋 전)
 *
 *     fileStorageService.uploadTo(thumbnail, FileCategory.CERTIFICATION, url); // 3. 실제 업로드는 맨 마지막
 *
 *     TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
 *         @Override
 *         public void afterCompletion(int status) {
 *             if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
 *                 fileStorageService.delete(url); // 4. 업로드 이후 커밋 자체가 실패하는 좁은 구간 대비
 *             }
 *             // STATUS_UNKNOWN은 의도적으로 제외 — 실제 커밋 성공 여부가 불확실한 상태라
 *             // 여기서 지우면 "DB엔 살아있는데 파일만 없는" 더 나쁜 상황이 될 수 있다.
 *         }
 *     });
 *
 *     return CreateCertificationPostResponse.from(post);
 * }
 * }</pre>
 *
 * <p>업로드 없이 url을 미리 발급받을 필요가 없는 간단한 경우(기존 row의 컬럼 하나만
 * 갱신하는 등, 미리 채워둘 새 row 자체가 없는 경우)엔 그냥 {@link #upload}를 먼저 호출해
 * url을 받은 다음 DB를 갱신해도 된다 — 이때도 "업로드 성공 후 DB 쓰기가 실패하는" 구간은
 * 동일하게 남으므로 위와 같은 {@link #delete} 보상 등록이 필요하다.</p>
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
     * 파일을 검증만 하고 실제로 업로드하지는 않은 채, 업로드했을 때 쓰일 URL을 미리
     * 발급한다. DB에 url을 먼저 저장해두고 실제 업로드는 맨 마지막에 하고 싶을 때
     * ({@link #uploadTo}와 짝으로) 쓴다.
     *
     * <p>여기서 발급한 url은 반드시 같은 {@code file}로 {@link #uploadTo}를 호출할 때
     * 그대로 넘겨야 한다 — 다른 파일이나 다른 url을 섞어 쓰면 안 된다.</p>
     *
     * @throws com.deundeun.global.exception.ApiException FILE_EMPTY — 파일이 비어있을 때
     * @throws com.deundeun.global.exception.ApiException FILE_TOO_LARGE — category별 최대 용량을 초과했을 때
     * @throws com.deundeun.global.exception.ApiException INVALID_FILE_TYPE — category가 허용하지 않는 타입일 때
     */
    String reserveUrl(MultipartFile file, FileCategory category);

    /**
     * {@link #reserveUrl}로 미리 발급받은 url이 가리키는 위치에 실제로 파일을 업로드한다.
     *
     * @throws com.deundeun.global.exception.ApiException FILE_EMPTY — 파일이 비어있을 때
     * @throws com.deundeun.global.exception.ApiException FILE_TOO_LARGE — category별 최대 용량을 초과했을 때
     * @throws com.deundeun.global.exception.ApiException INVALID_FILE_TYPE — category가 허용하지 않는 타입일 때
     * @throws com.deundeun.global.exception.ApiException INVALID_REQUEST — url이 {@link #reserveUrl}이 발급한
     *         형식이 아닐 때
     * @throws com.deundeun.global.exception.ApiException FILE_UPLOAD_FAILED — 스토리지 업로드 자체가 실패했을 때
     */
    void uploadTo(MultipartFile file, FileCategory category, String url);

    /**
     * 여러 파일을 한 번에 업로드하고 URL 목록을 반환한다(입력 순서 그대로). 업로드를 실제로
     * 시작하기 전에 전체 파일을 먼저 검증해서, 하나라도 실패하면 아무것도 업로드하지 않는다.
     * 각 파일에 대한 검증/업로드 자체는 {@link #upload}와 동일한 규칙을 그대로 따른다.
     *
     * <p>검증을 통과한 뒤에도 실제 업로드 도중 일부 파일만 성공하고 그다음 파일이 실패할 수
     * 있다(S3는 여러 객체를 하나로 묶어주지 않는다) — 이 경우 이미 성공한 파일들을 내부적으로
     * 다시 삭제하고 예외를 던진다. 다만 "전부 성공 아니면 전부 실패"는 이 메서드가
     * 반환하는 결과(정상 리스트 또는 예외)에만 해당하는 보장이다 — 이미 성공한 파일을
     * 지우는 그 삭제 자체도 {@link #delete}와 마찬가지로 best-effort라, 삭제가 실패하면
     * 예외는 던져지는데 S3에는 그 파일이 그대로 남는 상태가 될 수 있다. 즉 호출자 입장의
     * 리턴값은 항상 전부-아니면-전무이지만, 실제 스토리지 상태까지 그렇다는 보장은 아니라
     * 별도의 정합성 점검이 여전히 필요할 수 있다.</p>
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
