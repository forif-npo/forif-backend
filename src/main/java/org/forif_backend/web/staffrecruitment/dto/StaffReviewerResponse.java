package org.forif_backend.web.staffrecruitment.dto;

import jakarta.validation.constraints.NotNull;
import org.forif_backend.application.staffrecruitment.dto.StaffReviewerCandidateInfo;
import org.forif_backend.application.staffrecruitment.dto.StaffReviewerInfo;
import org.forif_backend.domain.staffrecruitment.StaffReviewerStatus;

import java.time.OffsetDateTime;

/**
 * 회차별 열람 권한 요청·응답.
 */
public final class StaffReviewerResponse {

    private StaffReviewerResponse() {
    }

    /** 초대 후보. ADMIN 계정이 없으면 목록에는 보이지만 부여 요청은 거절된다. */
    public record CandidateResponse(
            Long userId,
            String userName,
            String department,
            String affiliation,
            boolean hasAdminAccount,
            StaffReviewerStatus reviewerStatus
    ) {
        public static CandidateResponse from(StaffReviewerCandidateInfo info) {
            return new CandidateResponse(
                    info.userId(),
                    info.userName(),
                    info.department(),
                    info.affiliation(),
                    info.hasAdminAccount(),
                    info.reviewerStatus());
        }
    }

    public record ReviewerResponse(
            Long userId,
            String userName,
            String department,
            String affiliation,
            StaffReviewerStatus status,
            OffsetDateTime grantedAt,
            OffsetDateTime expiresAt,
            OffsetDateTime revokedAt
    ) {
        public static ReviewerResponse from(StaffReviewerInfo info) {
            return new ReviewerResponse(
                    info.userId(),
                    info.userName(),
                    info.department(),
                    info.affiliation(),
                    info.status(),
                    StaffRecruitmentResponse.toOffset(info.grantedAt()),
                    StaffRecruitmentResponse.toOffset(info.expiresAt()),
                    StaffRecruitmentResponse.toOffset(info.revokedAt()));
        }
    }

    /** 만료 시각은 필수다. 기본값을 서버가 정하지 않는다. */
    public record GrantRequest(
            @NotNull Long userId,
            @NotNull OffsetDateTime expiresAt
    ) {
    }
}
