package org.forif_backend.web.studyApply.dto;

import lombok.Builder;
import org.forif_backend.application.file.dto.FileInfo;

import java.util.List;

@Builder
public record CreateStudyApplyResponse(
        FileInfo thumbnailUploadInfo,
        List<FileInfo> referenceUploadInfos
) {
}
