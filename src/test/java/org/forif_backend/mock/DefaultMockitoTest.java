package org.forif_backend.mock;

import org.forif_backend.application.file.dto.FileInfo;
import org.forif_backend.application.file.port.out.FilePort;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = { // 테스트용 임의값
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379",
        "jwt.secret=D7s8f9G0h1j2k3L4z5x6c7v8b9n0m1Q2W3E4R5T6Y7u=",
        "notification.api-key=test-api-key",
        "notification.api-secret=test-api-secret",
        "notification.pf-id=test-pf-id",
        "notification.sender-number=01000000000"
})
@Transactional
@Rollback
public class DefaultMockitoTest {
    @MockitoBean
    protected FilePort filePort;

    @MockitoBean
    protected S3Client s3Client;

    @MockitoBean
    protected S3Presigner s3Presigner;

    @MockitoBean
    protected S3ClientBuilder s3ClientBuilder;

    @BeforeEach
    void setupMockFilePort() {
        // 기본 Mock 동작을 설정합니다.
        when(filePort.generatePresignedUploadUrl(any(MultipartFile.class)))
                .thenAnswer((Answer<FileInfo>) invocation -> {
                    // 인자로 받은 MultipartFile 객체 가져오기
                    MultipartFile file = invocation.getArgument(0);

                    // objectKey에 원본 파일명을 포함
                    String objectKey = "mock-uuid-" + file.getOriginalFilename();

                    return FileInfo.builder()
                            .objectKey(objectKey)
                            .presignedUrl("http://mock-s3-url.com/" + objectKey)
                            .build();
                });
    }
}
