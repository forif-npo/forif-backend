package org.forif_backend.application.studyApply.dto;

import lombok.Builder;
import org.forif_backend.application.file.dto.FileInfo;

import java.util.List;

@Builder
public record CreateStudyApplyInfo(
        FileInfo thumbnailUploadInfo,
        List<FileInfo> referenceUploadInfos
) {
}
