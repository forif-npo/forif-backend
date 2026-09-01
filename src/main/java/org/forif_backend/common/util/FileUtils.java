package org.forif_backend.common.util;

import lombok.experimental.UtilityClass;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@UtilityClass
public class FileUtils {
    public static String createFileName(MultipartFile file) {
        return "uploads/" + UUID.randomUUID().toString() + "-" + file.getOriginalFilename();
    }

}
