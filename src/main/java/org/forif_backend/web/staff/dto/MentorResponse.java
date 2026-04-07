package org.forif_backend.web.staff.dto;

import lombok.Builder;
import org.forif_backend.domain.staff.StaffAccount;

@Builder
public record MentorResponse(
    Long userId,
    String name,
    String department,
    String phoneNum,
    String studyName
) {
    public static MentorResponse from(StaffAccount staffAccount) {
        return MentorResponse.builder()
                .userId(staffAccount.getUserId())
                .name(staffAccount.getName())
                .department(staffAccount.getUser().getDepartment())
                .phoneNum(staffAccount.getUser().getPhoneNum())
                .studyName(staffAccount.getAffiliation())
                .build();
    }
}
