package com.taskflow.backend.domain.attachment.service;

import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class S3TaskAttachmentStorageTest {

    @Mock
    private S3Client s3Client;

    @Test
    void storeUploadsFileToS3AndReturnsStoredMetadata() {
        S3TaskAttachmentStorage storage = new S3TaskAttachmentStorage(
                s3Client,
                "taskflow-attachments",
                "task-attachments"
        );
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "report.pdf",
                "application/pdf",
                "hello".getBytes()
        );

        TaskAttachmentStorage.StoredAttachment stored = storage.store(10L, file);

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
        PutObjectRequest request = requestCaptor.getValue();
        assertThat(request.bucket()).isEqualTo("taskflow-attachments");
        assertThat(request.key()).startsWith("task-attachments/10/");
        assertThat(request.key()).endsWith(".pdf");
        assertThat(request.contentType()).isEqualTo("application/pdf");
        assertThat(request.acl()).isNull();
        assertThat(request.contentDisposition()).contains("attachment");

        assertThat(stored.storagePath()).isEqualTo(request.key());
        assertThat(stored.storedFilename()).isNotBlank();
        assertThat(stored.contentType()).isEqualTo("application/pdf");
        assertThat(stored.sizeBytes()).isEqualTo(5L);
    }

    @Test
    void deleteRemovesObjectFromS3() {
        S3TaskAttachmentStorage storage = new S3TaskAttachmentStorage(
                s3Client,
                "taskflow-attachments",
                "task-attachments"
        );

        storage.delete("task-attachments/10/report.pdf");

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        assertThat(captor.getValue().bucket()).isEqualTo("taskflow-attachments");
        assertThat(captor.getValue().key()).isEqualTo("task-attachments/10/report.pdf");
    }

    @Test
    void storeThrowsBusinessExceptionWhenS3UploadFails() {
        S3TaskAttachmentStorage storage = new S3TaskAttachmentStorage(
                s3Client,
                "taskflow-attachments",
                "task-attachments"
        );
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "report.pdf",
                "application/pdf",
                "hello".getBytes()
        );
        doThrow(new RuntimeException("s3 down"))
                .when(s3Client)
                .putObject(any(PutObjectRequest.class), any(RequestBody.class));

        assertThatThrownBy(() -> storage.store(10L, file))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FILE_UPLOAD_FAILED);
    }
}
