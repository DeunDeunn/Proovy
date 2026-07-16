package com.deundeun.global.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class TransactionalFileUploaderTest {

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private TransactionalFileUploader uploader;

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private void completeTransaction(int status) {
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(sync -> sync.afterCompletion(status));
    }

    @Test
    void upload_withoutActiveTransaction_throwsIllegalStateException() {
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1});

        assertThatThrownBy(() -> uploader.upload(file, FileCategory.PROFILE))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void upload_withinTransaction_delegatesAndReturnsUrl() {
        TransactionSynchronizationManager.initSynchronization();
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1});
        when(fileStorageService.upload(file, FileCategory.PROFILE)).thenReturn("https://s3/profile/a.png");

        String url = uploader.upload(file, FileCategory.PROFILE);

        assertThat(url).isEqualTo("https://s3/profile/a.png");
    }

    @Test
    void upload_transactionRolledBack_deletesUploadedFile() {
        TransactionSynchronizationManager.initSynchronization();
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1});
        when(fileStorageService.upload(file, FileCategory.PROFILE)).thenReturn("https://s3/profile/a.png");

        uploader.upload(file, FileCategory.PROFILE);
        completeTransaction(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(fileStorageService).delete("https://s3/profile/a.png");
    }

    @Test
    void upload_transactionCommitted_doesNotDeleteUploadedFile() {
        TransactionSynchronizationManager.initSynchronization();
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1});
        when(fileStorageService.upload(file, FileCategory.PROFILE)).thenReturn("https://s3/profile/a.png");

        uploader.upload(file, FileCategory.PROFILE);
        completeTransaction(TransactionSynchronization.STATUS_COMMITTED);

        verify(fileStorageService, never()).delete(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void uploadAll_withoutActiveTransaction_throwsIllegalStateException() {
        List<org.springframework.web.multipart.MultipartFile> files = List.of(
                new MockMultipartFile("file", "a.png", "image/png", new byte[]{1}));

        assertThatThrownBy(() -> uploader.uploadAll(files, FileCategory.CERTIFICATION))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void uploadAll_transactionRolledBack_deletesAllUploadedFiles() {
        TransactionSynchronizationManager.initSynchronization();
        MockMultipartFile file1 = new MockMultipartFile("files", "a.png", "image/png", new byte[]{1});
        MockMultipartFile file2 = new MockMultipartFile("files", "b.png", "image/png", new byte[]{2});
        List<org.springframework.web.multipart.MultipartFile> files = List.of(file1, file2);
        when(fileStorageService.uploadAll(files, FileCategory.CERTIFICATION))
                .thenReturn(List.of("https://s3/certification/a.png", "https://s3/certification/b.png"));

        uploader.uploadAll(files, FileCategory.CERTIFICATION);
        completeTransaction(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(fileStorageService).delete("https://s3/certification/a.png");
        verify(fileStorageService).delete("https://s3/certification/b.png");
    }

    @Test
    void uploadReplacing_committedWithOldUrl_deletesOldUrlOnly() {
        TransactionSynchronizationManager.initSynchronization();
        MockMultipartFile file = new MockMultipartFile("file", "new.png", "image/png", new byte[]{1});
        when(fileStorageService.upload(file, FileCategory.PROFILE)).thenReturn("https://s3/profile/new.png");

        String newUrl = uploader.uploadReplacing(file, FileCategory.PROFILE, "https://s3/profile/old.png");
        completeTransaction(TransactionSynchronization.STATUS_COMMITTED);

        assertThat(newUrl).isEqualTo("https://s3/profile/new.png");
        verify(fileStorageService).delete("https://s3/profile/old.png");
        verify(fileStorageService, never()).delete("https://s3/profile/new.png");
    }

    @Test
    void uploadReplacing_rolledBack_deletesNewUrlOnly() {
        TransactionSynchronizationManager.initSynchronization();
        MockMultipartFile file = new MockMultipartFile("file", "new.png", "image/png", new byte[]{1});
        when(fileStorageService.upload(file, FileCategory.PROFILE)).thenReturn("https://s3/profile/new.png");

        uploader.uploadReplacing(file, FileCategory.PROFILE, "https://s3/profile/old.png");
        completeTransaction(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(fileStorageService).delete("https://s3/profile/new.png");
        verify(fileStorageService, never()).delete("https://s3/profile/old.png");
    }

    @Test
    void uploadReplacing_committedWithNullOldUrl_deletesNothing() {
        TransactionSynchronizationManager.initSynchronization();
        MockMultipartFile file = new MockMultipartFile("file", "new.png", "image/png", new byte[]{1});
        when(fileStorageService.upload(file, FileCategory.PROFILE)).thenReturn("https://s3/profile/new.png");

        uploader.uploadReplacing(file, FileCategory.PROFILE, null);
        completeTransaction(TransactionSynchronization.STATUS_COMMITTED);

        verify(fileStorageService, never()).delete(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void uploadReplacing_statusUnknown_deletesNothing() {
        TransactionSynchronizationManager.initSynchronization();
        MockMultipartFile file = new MockMultipartFile("file", "new.png", "image/png", new byte[]{1});
        when(fileStorageService.upload(file, FileCategory.PROFILE)).thenReturn("https://s3/profile/new.png");

        uploader.uploadReplacing(file, FileCategory.PROFILE, "https://s3/profile/old.png");
        completeTransaction(TransactionSynchronization.STATUS_UNKNOWN);

        verify(fileStorageService, never()).delete(org.mockito.ArgumentMatchers.anyString());
    }
}
