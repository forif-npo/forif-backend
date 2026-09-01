package org.forif_backend.application.file.port.out;

import org.forif_backend.application.file.dto.FileInfo;
import org.springframework.web.multipart.MultipartFile;

public interface FilePort {
    FileInfo generatePresignedUploadUrl(MultipartFile file);
    FileInfo generatePresignedViewUrl(String objectKey);
    String uploadFile(MultipartFile file);
    default String uploadFile(MultipartFile file, String directory) {
        return uploadFile(file);
    }

    /**
     * 서버에서 생성한 파일(수료증 이미지 등)을 저장하고 objectKey를 반환한다.
     */
    String uploadBytes(byte[] content, String filename, String directory, String contentType);

    /**
     * 저장된 파일을 바이트로 읽어온다 (수료증 서명 합성 등 서버 내 처리용).
     */
    byte[] downloadBytes(String objectKey);

    default void createDirectory(String directory) {
    }
    void deleteFile(String objectKey);
}
