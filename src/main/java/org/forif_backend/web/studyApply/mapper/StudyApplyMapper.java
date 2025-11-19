package org.forif_backend.web.studyApply.mapper;


import org.forif_backend.application.studyApply.dto.CreateStudyApplyInfo;
import org.forif_backend.web.studyApply.dto.CreateStudyApplyResponse;

public class StudyApplyMapper {
    public static CreateStudyApplyResponse from(CreateStudyApplyInfo info) {
        return CreateStudyApplyResponse.builder()
                .referenceUploadInfos(info.referenceUploadInfos())
                .thumbnailUploadInfo(info.thumbnailUploadInfo())
                .build();
    }
}
