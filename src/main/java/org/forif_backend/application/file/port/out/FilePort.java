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
    default void createDirectory(String directory) {
    }
    void deleteFile(String objectKey);
}
