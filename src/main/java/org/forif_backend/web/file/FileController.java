package org.forif_backend.web.file;

import jakarta.servlet.http.HttpServletRequest;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriUtils;

import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@ConditionalOnProperty(name = "app.file.storage", havingValue = "local", matchIfMissing = true)
public class FileController {

    @Value("${app.file.local.root-path:./storage/uploads}")
    private String rootPath;

    @GetMapping("/api/v1/files/**")
    public ResponseEntity<Resource> getFile(HttpServletRequest request) {
        String objectKey = extractObjectKey(request);
        if (!StringUtils.hasText(objectKey)) {
            throw new ForifException(ErrorCode.FILE_NOT_FOUND);
        }

        Path root = Paths.get(rootPath).toAbsolutePath().normalize();
        Path filePath = root.resolve(objectKey).normalize();
        if (!filePath.startsWith(root) || !Files.isRegularFile(filePath)) {
            throw new ForifException(ErrorCode.FILE_NOT_FOUND);
        }

        try {
            Resource resource = new UrlResource(filePath.toUri());
            MediaType mediaType = MediaTypeFactory.getMediaType(resource)
                    .orElse(MediaType.APPLICATION_OCTET_STREAM);
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .body(resource);
        } catch (MalformedURLException e) {
            throw new ForifException(ErrorCode.FILE_NOT_FOUND);
        }
    }

    private String extractObjectKey(HttpServletRequest request) {
        String prefix = "/api/v1/files/";
        String requestUri = request.getRequestURI();
        if (!requestUri.startsWith(prefix)) {
            return "";
        }
        return UriUtils.decode(requestUri.substring(prefix.length()), StandardCharsets.UTF_8);
    }
}
