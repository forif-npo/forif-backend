package org.forif_backend.web.user.dto;

import lombok.Builder;
import org.forif_backend.application.user.dto.MemberInfo;
import org.forif_backend.common.dto.response.CursorPageResponse;

@Builder
public record MemberResponse(
        Long userId,
        String department,
        String userName,
        String phoneNum,
        String currentStudyName,
        boolean isMentor,
        boolean isAdmin
) {
    public static MemberResponse from(MemberInfo info) {
        return MemberResponse.builder()
                .userId(info.userId())
                .department(info.department())
                .userName(info.userName())
                .phoneNum(info.phoneNum())
                .currentStudyName(info.currentStudyName())
                .isMentor(info.isMentor())
                .isAdmin(info.isAdmin())
                .build();
    }

    public static CursorPageResponse<MemberResponse> fromPage(CursorPageResponse<MemberInfo> page) {
        return page.withContent(page.content().stream().map(MemberResponse::from).toList());
    }
}
