package org.forif_backend.infrastructure.external.file;

import org.assertj.core.api.Assertions;
import org.forif_backend.application.file.dto.FileInfo;
import org.forif_backend.application.file.port.out.FilePort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@SpringBootTest
public class FileUploadTest {
    @Autowired
    private FilePort filePort;

    @Test
    void file_upload_test() {

        // 목업 파일 생성
        MultipartFile mockFile = new MockMultipartFile(
                "file",
                "test.jpeg",
                "image/jpeg",     // 콘텐츠 타입
                "test".getBytes()
        );

        // 업로드 url 요청
        FileInfo fileInfo = filePort.generatePresignedUploadUrl(mockFile);
        System.out.println(fileInfo.presignedUrl());

        // 검증
        Assertions.assertThat(fileInfo.presignedUrl()).isNotNull();
    }
}
