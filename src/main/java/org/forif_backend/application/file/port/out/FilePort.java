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

    default void createDirectory(String directory) {
    }
    void deleteFile(String objectKey);
}
