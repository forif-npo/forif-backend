package org.forif_backend.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 400 Bad Request
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "FOR001-400", "잘못된 요청입니다."),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "FOR002-400", "입력값이 유효하지 않습니다."),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "FOR003-400", "입력값 검증에 실패했습니다."),
    MISSING_PARAMETER(HttpStatus.BAD_REQUEST, "FOR004-400", "필수 파라미터가 누락되었습니다."),
    TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "FOR005-400", "파라미터 타입이 올바르지 않습니다."),
    STUDY_ALREADY_APPROVED(HttpStatus.BAD_REQUEST, "FOR006-400", "이미 승인된 스터디입니다."),
    PRIMARY_STUDY_REQUIRED(HttpStatus.BAD_REQUEST, "FOR007-400", "1순위 스터디를 무조건 선택해야 합니다."),
    USER_NOT_APPLIED_TO_STUDY(HttpStatus.BAD_REQUEST, "FOR008-400", "해당 스터디에 지원하지 않은 유저입니다."),
    STUDY_APPLICATION_PERIOD_ENDED(HttpStatus.BAD_REQUEST, "FOR009-400", "스터디 지원 기간이 아닙니다."),
    ALREADY_APPLIED_PRIMARY(HttpStatus.BAD_REQUEST, "FOR010-400", "이미 1순위 스터디에 지원했습니다."),
    ALREADY_APPLIED_SECONDARY(HttpStatus.BAD_REQUEST, "FOR011-400", "이미 2순위 스터디에 지원했습니다."),
    PRIMARY_NOT_APPLIED(HttpStatus.BAD_REQUEST, "FOR037-400", "1순위 스터디에 먼저 지원해야 합니다."),
    REJECT_REASON_REQUIRED(HttpStatus.BAD_REQUEST, "FOR012-400", "거절 사유는 필수입니다."),
    APPLY_NOT_PENDING(HttpStatus.BAD_REQUEST, "FOR038-400", "대기중(PENDING) 상태의 신청서만 수정할 수 있습니다."),
    REAPPLY_ONLY_FOR_REJECTED(HttpStatus.BAD_REQUEST, "FOR013-400", "재요청은 거절된 신청서에만 가능합니다."),
    INVALID_FILE_ATTACHMENT(HttpStatus.BAD_REQUEST, "FOR014-400", "파일 첨부가 잘못됐습니다."),
    INVALID_EMAIL_DOMAIN(HttpStatus.BAD_REQUEST, "FOR015-400", "한양대 이메일(@hanyang.ac.kr)만 사용 가능합니다."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "FOR016-400", "비밀번호가 일치하지 않습니다."),

    // 401 Unauthorized
    MISSING_TOKEN(HttpStatus.UNAUTHORIZED, "FOR017-401", "토큰이 없습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "FOR018-401", "유효하지 않은 토큰입니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "FOR019-401", "토큰이 만료되었습니다."),

    // 403 Forbidden
    INSUFFICIENT_PERMISSION(HttpStatus.FORBIDDEN, "FOR020-403", "권한이 없습니다."),
    NOT_STUDY_MENTOR(HttpStatus.FORBIDDEN, "FOR021-403", "해당 스터디의 멘토가 아닙니다."),

    // 404 Not Found
    STUDY_NOT_FOUND(HttpStatus.NOT_FOUND, "FOR022-404", "스터디가 존재하지 않습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "FOR023-404", "유저를 찾을 수 없습니다."),
    NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "FOR024-404", "공지사항이 없습니다."),
    SPECIFIC_NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "FOR025-404", "해당 공지사항이 없습니다."),
    FAQ_NOT_FOUND(HttpStatus.NOT_FOUND, "FOR026-404", "FAQ가 없습니다."),
    SPECIFIC_FAQ_NOT_FOUND(HttpStatus.NOT_FOUND, "FOR027-404", "해당 FAQ가 없습니다."),
    TECH_BLOG_NOT_FOUND(HttpStatus.NOT_FOUND, "FOR028-404", "기술 블로그가 없습니다."),
    SPECIFIC_TECH_BLOG_NOT_FOUND(HttpStatus.NOT_FOUND, "FOR029-404", "해당 기술 블로그 글이 없습니다."),
    STUDY_APPLY_NOT_FOUND(HttpStatus.NOT_FOUND, "FOR030-404", "해당하는 스터디 신청을 찾을 수 없습니다."),
    FIRST_MENTOR_NOT_FOUND(HttpStatus.NOT_FOUND, "FOR031-404", "1순위 멘토를 찾을 수 없습니다."),
    SECOND_MENTOR_NOT_FOUND(HttpStatus.NOT_FOUND, "FOR032-404", "2순위 멘토를 찾을 수 없습니다."),
    STUDY_PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "FOR033-404", "스터디 플랜이 없습니다."),
    WEEKLY_PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "FOR034-404", "해당하는 스터디의 주간 계획을 찾을 수 없습니다."),
    STAFF_NOT_FOUND(HttpStatus.NOT_FOUND, "FOR035-404", "등록되지 않은 스태프입니다."),
    CERTIFICATE_NOT_ISSUED(HttpStatus.NOT_FOUND, "FOR036-404", "인증서가 발급되지 않았습니다."),
    SEMESTER_SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "FOR042-404", "해당 학기 일정을 찾을 수 없습니다."),
    FORIF_TEAM_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "FOR043-404", "해당 운영진 이력을 찾을 수 없습니다."),

    // 409 Conflict
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "FOR044-409", "이미 가입된 사용자입니다."),
    USER_APPLY_ALREADY_EXISTS(HttpStatus.CONFLICT, "FOR045-409", "이번학기 스터디에 이미 지원 했습니다."),
    STAFF_ALREADY_EXISTS(HttpStatus.CONFLICT, "FOR046-409", "이미 등록된 스태프 계정입니다."),
    STUDENT_ID_ALREADY_EXISTS(HttpStatus.CONFLICT, "FOR047-409", "이미 가입된 학번입니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "FOR048-409", "이미 가입된 이메일입니다."),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "FOR100-500", "서버 내부 오류가 발생했습니다."),
    INVALID_STATUS_VALUE(HttpStatus.INTERNAL_SERVER_ERROR, "FOR101-500", "유효하지 않은 상태 값입니다."),
    NOTIFICATION_TEMPLATE_FETCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FOR102-500", "알림톡 템플릿 조회에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
