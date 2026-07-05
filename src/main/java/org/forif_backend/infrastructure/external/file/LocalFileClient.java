package org.forif_backend.infrastructure.external.file;

import lombok.extern.slf4j.Slf4j;
import org.forif_backend.application.file.dto.FileInfo;
import org.forif_backend.application.file.port.out.FilePort;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.file.storage", havingValue = "local", matchIfMissing = true)
public class LocalFileClient implements FilePort {

    private static final String FILE_API_PATH = "/api/v1/files";

    private final Path rootPath;
    private final String publicUrl;

    public LocalFileClient(
            @Value("${app.file.local.root-path:./storage/uploads}") String rootPath,
            @Value("${app.file.local.public-url:}") String publicUrl
    ) {
        this.rootPath = Paths.get(rootPath).toAbsolutePath().normalize();
        this.publicUrl = stripTrailingSlash(publicUrl);

        try {
            Files.createDirectories(this.rootPath);
        } catch (IOException e) {
            log.error("로컬 파일 저장소 디렉터리 생성 실패: {}", this.rootPath, e);
            throw new ForifException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public FileInfo generatePresignedUploadUrl(MultipartFile file) {
        String objectKey = uploadFile(file);
        return generatePresignedViewUrl(objectKey);
    }

    @Override
    public FileInfo generatePresignedViewUrl(String objectKey) {
        return FileInfo.builder()
                .objectKey(objectKey)
                .presignedUrl(buildViewUrl(objectKey))
                .build();
    }

    @Override
    public String uploadFile(MultipartFile file) {
        return uploadFile(file, null);
    }

    @Override
    public String uploadFile(MultipartFile file, String directory) {
        if (file == null || file.isEmpty()) {
            throw new ForifException(ErrorCode.INVALID_FILE_ATTACHMENT);
        }

        String objectKey = createObjectKey(file, directory);
        Path targetPath = resolvePath(objectKey);

        try {
            Files.createDirectories(targetPath.getParent());
            file.transferTo(targetPath);
            log.info("로컬 파일 업로드 성공: {}", objectKey);
            return objectKey;
        } catch (IOException e) {
            log.error("로컬 파일 업로드 중 오류 발생: {}", objectKey, e);
            throw new ForifException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public String uploadBytes(byte[] content, String filename, String directory, String contentType) {
        if (content == null || content.length == 0) {
            throw new ForifException(ErrorCode.INVALID_FILE_ATTACHMENT);
        }

        String safeFilename = StringUtils.cleanPath(filename).replace("\\", "_").replace("/", "_");
        String normalizedDirectory = normalizeDirectory(directory);
        String objectKey = StringUtils.hasText(normalizedDirectory)
                ? normalizedDirectory + "/" + safeFilename
                : safeFilename;
        Path targetPath = resolvePath(objectKey);

        try {
            Files.createDirectories(targetPath.getParent());
            Files.write(targetPath, content);
            log.info("로컬 파일 저장 성공: {}", objectKey);
            return objectKey;
        } catch (IOException e) {
            log.error("로컬 파일 저장 중 오류 발생: {}", objectKey, e);
            throw new ForifException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public byte[] downloadBytes(String objectKey) {
        Path targetPath = resolvePath(objectKey);
        if (!Files.exists(targetPath)) {
            throw new ForifException(ErrorCode.INVALID_FILE_ATTACHMENT);
        }
        try {
            return Files.readAllBytes(targetPath);
        } catch (IOException e) {
            log.error("로컬 파일 읽기 중 오류 발생: {}", objectKey, e);
            throw new ForifException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void createDirectory(String directory) {
        Path directoryPath = resolveDirectoryPath(directory);
        try {
            Files.createDirectories(directoryPath);
            log.info("로컬 파일 저장소 디렉터리 준비 완료: {}", directoryPath);
        } catch (IOException e) {
            log.error("로컬 파일 저장소 디렉터리 생성 실패: {}", directoryPath, e);
            throw new ForifException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void deleteFile(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            return;
        }

        try {
            boolean deleted = Files.deleteIfExists(resolvePath(objectKey));
            if (deleted) {
                log.info("로컬 파일 삭제 성공: {}", objectKey);
            }
        } catch (IOException e) {
            log.error("로컬 파일 삭제 중 오류 발생 (ObjectKey: {})", objectKey, e);
            throw new ForifException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private Path resolvePath(String objectKey) {
        Path targetPath = rootPath.resolve(objectKey).normalize();
        if (!targetPath.startsWith(rootPath)) {
            throw new ForifException(ErrorCode.INVALID_FILE_ATTACHMENT);
        }
        return targetPath;
    }

    private Path resolveDirectoryPath(String directory) {
        Path directoryPath = rootPath.resolve(normalizeDirectory(directory)).normalize();
        if (!directoryPath.startsWith(rootPath)) {
            throw new ForifException(ErrorCode.INVALID_FILE_ATTACHMENT);
        }
        return directoryPath;
    }

    private String createObjectKey(MultipartFile file, String directory) {
        String originalFilename = Optional.ofNullable(file.getOriginalFilename())
                .filter(StringUtils::hasText)
                .map(StringUtils::cleanPath)
                .orElse("file");
        String safeFilename = originalFilename.replace("\\", "_").replace("/", "_");
        String filename = UUID.randomUUID() + "-" + safeFilename;
        String normalizedDirectory = normalizeDirectory(directory);
        return StringUtils.hasText(normalizedDirectory)
                ? normalizedDirectory + "/" + filename
                : filename;
    }

    private String normalizeDirectory(String directory) {
        if (!StringUtils.hasText(directory)) {
            return "";
        }

        String normalized = StringUtils.cleanPath(directory).replace("\\", "/");
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.contains("..")) {
            throw new ForifException(ErrorCode.INVALID_FILE_ATTACHMENT);
        }
        return normalized;
    }

    private String buildViewUrl(String objectKey) {
        String encodedObjectKey = UriUtils.encodePath(objectKey, StandardCharsets.UTF_8);
        if (StringUtils.hasText(publicUrl)) {
            return publicUrl + "/" + encodedObjectKey;
        }

        try {
            return ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path(FILE_API_PATH)
                    .path("/")
                    .path(encodedObjectKey)
                    .build()
                    .toUriString();
        } catch (IllegalStateException e) {
            return FILE_API_PATH + "/" + encodedObjectKey;
        }
    }

    private String stripTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
