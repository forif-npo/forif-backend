package org.forif_backend.web.study.mapper;


import org.forif_backend.application.study.dto.CreateStudyApplyInfo;
import org.forif_backend.web.study.dto.CreateStudyApplyResponse;

public class StudyApplyMapper {
    public static CreateStudyApplyResponse from(CreateStudyApplyInfo info) {
        return CreateStudyApplyResponse.builder()
                .referenceUploadInfos(info.referenceUploadInfos())
                .thumbnailUploadInfo(info.thumbnailUploadInfo())
                .build();
    }
}
