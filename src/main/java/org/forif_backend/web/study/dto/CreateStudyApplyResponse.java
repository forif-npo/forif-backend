package org.forif_backend.web.study.dto;

import lombok.Builder;
import org.forif_backend.application.file.dto.FileInfo;
import org.forif_backend.application.study.dto.CreateStudyApplyInfo;

import java.util.List;

public record CreateStudyApplyResponse(
        FileInfo thumbnailUploadInfo,
        List<FileInfo> referenceUploadInfos
) {
    // 매퍼 역할을 DTO 내부로 흡수
    public static CreateStudyApplyResponse from(CreateStudyApplyInfo info) {
        return new CreateStudyApplyResponse(
                info.thumbnailUploadInfo(),
                info.referenceUploadInfos()
        );
    }
}