package org.forif_backend.infrastructure.external.file;

import org.forif_backend.application.file.dto.FileInfo;
import org.forif_backend.application.file.port.out.FilePort;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class S3FileClient implements FilePort {

    @Override
    public FileInfo generatePresignedUploadUrl(MultipartFile file) {
        return FileInfo.builder().build();
    }

    @Override
    public FileInfo generatePresignedViewUrl(String objectKey) {
        return FileInfo.builder().build();
    }
}
