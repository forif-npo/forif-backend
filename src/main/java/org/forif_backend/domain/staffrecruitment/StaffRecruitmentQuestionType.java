package org.forif_backend.domain.staffrecruitment;

/**
 * 자기소개서 문항 타입.
 */
public enum StaffRecruitmentQuestionType {
    SHORT_TEXT,  // 한 줄 입력
    LONG_TEXT,   // 여러 줄 입력
    LINK,        // URL 입력
    FILE,        // 첨부 파일
    ATTACHMENT   // 링크 또는 첨부 파일 중 하나
}
