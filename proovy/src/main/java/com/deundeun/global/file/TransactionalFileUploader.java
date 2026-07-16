package com.deundeun.global.file;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * {@link FileStorageService}로 업로드한 뒤, 현재 트랜잭션이 롤백되면 그 파일을 자동으로
 * 지우도록 예약까지 해주는 파사드. 도메인 서비스가 {@link TransactionSynchronizationManager}를
 * 직접 다루지 않고, 이 클래스의 메서드 호출 한 줄로 업로드+보상 등록을 끝낼 수 있게 한다.
 *
 * <p>반드시 {@code @Transactional} 메서드 안에서 호출해야 한다 — 활성 트랜잭션이 없으면
 * 안전망이 조용히 빠진 채 넘어가는 대신 즉시 예외를 던진다.</p>
 */
@Component
@RequiredArgsConstructor
public class TransactionalFileUploader {

    private final FileStorageService fileStorageService;

    /**
     * 파일 하나를 업로드하고, 트랜잭션이 롤백되면 자동으로 지우도록 예약한다.
     */
    public String upload(MultipartFile file, FileCategory category) {
        requireActiveTransaction();
        String url = fileStorageService.upload(file, category);
        registerDeleteOnRollback(url);
        return url;
    }

    /**
     * 여러 파일을 업로드하고, 트랜잭션이 롤백되면 전부 자동으로 지우도록 예약한다.
     */
    public List<String> uploadAll(List<MultipartFile> files, FileCategory category) {
        requireActiveTransaction();
        List<String> urls = fileStorageService.uploadAll(files, category);
        urls.forEach(this::registerDeleteOnRollback);
        return urls;
    }

    /**
     * 기존 파일을 새 파일로 교체할 때 쓴다(프로필 이미지 변경 등). 트랜잭션이 커밋되면
     * 더 이상 쓰이지 않는 {@code oldUrl}을, 롤백되면 방금 올린 새 파일을 자동으로 지운다.
     *
     * @param oldUrl 교체 전 파일의 url. 처음 설정하는 경우라 없으면 {@code null}.
     */
    public String uploadReplacing(MultipartFile file, FileCategory category, String oldUrl) {
        requireActiveTransaction();
        String newUrl = fileStorageService.upload(file, category);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_COMMITTED && oldUrl != null) {
                    fileStorageService.delete(oldUrl);
                } else if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    fileStorageService.delete(newUrl);
                }
                // STATUS_UNKNOWN은 의도적으로 제외 — 실제 커밋 성공 여부가 불확실한 상태라
                // 여기서 지우면 orphan 파일보다 더 나쁜 "깨진 링크"가 생길 수 있다.
            }
        });
        return newUrl;
    }

    private void registerDeleteOnRollback(String url) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    fileStorageService.delete(url);
                }
            }
        });
    }

    private void requireActiveTransaction() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException(
                    "TransactionalFileUploader는 @Transactional 메서드 안에서만 호출할 수 있습니다.");
        }
    }
}
