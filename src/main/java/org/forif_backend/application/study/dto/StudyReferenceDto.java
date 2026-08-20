package org.forif_backend.application.study.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.study.ReferenceType;
import org.forif_backend.domain.study.StudyReference;

import java.util.UUID;

@Getter
@RequiredArgsConstructor
@Builder
public class StudyReferenceDto {
    private final UUID id;
    private final ReferenceType referenceType;
    private final String content;
    private final String fileName;

    public static StudyReferenceDto from(StudyReference reference) {
        return from(reference, reference.getContent());
    }

    public static StudyReferenceDto from(StudyReference reference, String content) {
        return StudyReferenceDto.builder()
                .id(reference.getId())
                .referenceType(reference.getReferenceType())
                .content(content)
                .fileName(resolveFileName(reference))
                .build();
    }

    private static String resolveFileName(StudyReference reference) {
        if (reference.getReferenceType() != ReferenceType.FILE) {
            return null;
        }

        String objectKey = reference.getContent();
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }

        String fileName = objectKey.substring(objectKey.lastIndexOf('/') + 1);
        int queryIndex = fileName.indexOf('?');
        if (queryIndex >= 0) {
            fileName = fileName.substring(0, queryIndex);
        }

        // 저장소 객체 키는 "UUID-원본파일명" 형식이다. 사용자에게는 원본 파일명만 노출한다.
        if (fileName.length() > 37 && fileName.charAt(36) == '-') {
            try {
                UUID.fromString(fileName.substring(0, 36));
                return fileName.substring(37);
            } catch (IllegalArgumentException ignored) {
                // UUID 접두사가 아닌 기존 객체 키는 그대로 사용한다.
            }
        }

        return fileName;
    }
}
