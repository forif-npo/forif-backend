package org.forif_backend.application.study.dto;

import lombok.Builder;
import org.forif_backend.application.file.dto.FileInfo;

import java.util.List;

@Builder
public record CreateStudyApplyInfo(
        Integer studyId,
        FileInfo thumbnailUploadInfo,
        List<FileInfo> referenceUploadInfos
) {
}
