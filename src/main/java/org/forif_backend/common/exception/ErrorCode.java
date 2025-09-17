package org.forif_backend.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    
    // 400 Bad Request
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "FOR001-400", "잘못된 요청입니다."),
    INVALID_SEMESTER_TEAM(HttpStatus.BAD_REQUEST, "FOR002-400", "해당 연도, 학기에 해당하는 운영진이 없습니다."),
    INVALID_STUDENT_ID(HttpStatus.BAD_REQUEST, "FOR003-400", "해당 학번에 해당하는 운영진이 없습니다."),
    INVALID_USER_ID(HttpStatus.BAD_REQUEST, "FOR004-400", "ID에 해당하는 유저가 없습니다."),
    INVALID_SEMESTER_INFO(HttpStatus.BAD_REQUEST, "FOR005-400", "해당 학기의 정보가 존재하지 않습니다."),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "FOR006-400", "입력값이 유효하지 않습니다."),
    STUDY_BAD_REQUEST(HttpStatus.BAD_REQUEST, "FOR007-400", "요청한 스터디가 존재하지 않습니다."),
    INVALID_APPLICATION(HttpStatus.BAD_REQUEST, "FOR008-400", "지원서가 없습니다."),
    STUDY_ALREADY_APPROVED(HttpStatus.BAD_REQUEST, "FOR009-400", "이미 승인된 스터디입니다."),
    PRIMARY_STUDY_REQUIRED(HttpStatus.BAD_REQUEST, "FOR010-400", "1순위 스터디를 무조건 선택해야 합니다."),
    USER_NOT_APPLIED_TO_STUDY(HttpStatus.BAD_REQUEST, "FOR011-400", "해당 스터디에 지원하지 않은 유저입니다."),
    STUDY_APPLICATION_PERIOD_ENDED(HttpStatus.BAD_REQUEST, "FOR012-400", "스터디 지원 기간이 아닙니다."),
    
    // 401 Unauthorized
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "FOR013-401", "유효하지 않은 토큰입니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "FOR014-401", "토큰이 만료되었습니다."),
    UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED, "FOR015-401", "권한이 없습니다."),
    
    // 403 Forbidden
    INSUFFICIENT_PERMISSION(HttpStatus.FORBIDDEN, "FOR016-403", "권한이 없습니다."),
    NOT_STUDY_MENTOR(HttpStatus.FORBIDDEN, "FOR017-403", "해당 스터디의 멘토가 아닙니다."),
    
    // 404 Not Found
    STUDY_NOT_FOUND(HttpStatus.NOT_FOUND, "FOR018-404", "스터디가 존재하지 않습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "FOR019-404", "유저를 찾을 수 없습니다."),
    NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "FOR020-404", "공지사항이 없습니다."),
    SPECIFIC_NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "FOR021-404", "해당 공지사항이 없습니다."),
    FAQ_NOT_FOUND(HttpStatus.NOT_FOUND, "FOR022-404", "FAQ가 없습니다."),
    SPECIFIC_FAQ_NOT_FOUND(HttpStatus.NOT_FOUND, "FOR023-404", "해당 FAQ가 없습니다."),
    TECH_BLOG_NOT_FOUND(HttpStatus.NOT_FOUND, "FOR024-404", "기술 블로그가 없습니다."),
    SPECIFIC_TECH_BLOG_NOT_FOUND(HttpStatus.NOT_FOUND, "FOR025-404", "해당 기술 블로그 글이 없습니다."),
    STUDY_APPLY_NOT_FOUND(HttpStatus.NOT_FOUND, "FOR026-404", "해당하는 스터디 신청을 찾을 수 없습니다."),
    FIRST_MENTOR_NOT_FOUND(HttpStatus.NOT_FOUND, "FOR027-404", "1순위 멘토를 찾을 수 없습니다."),
    SECOND_MENTOR_NOT_FOUND(HttpStatus.NOT_FOUND, "FOR028-404", "2순위 멘토를 찾을 수 없습니다."),
    STUDY_PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "FOR029-404", "스터디 플랜이 없습니다."),
    WEEKLY_PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "FOR030-404", "해당하는 스터디의 주간 계획을 찾을 수 없습니다."),
    
    // 409 Conflict
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "FOR031-409", "이미 가입된 사용자입니다."),
    
    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "FOR101-500", "서버 내부 오류가 발생했습니다.");
    
    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
} 