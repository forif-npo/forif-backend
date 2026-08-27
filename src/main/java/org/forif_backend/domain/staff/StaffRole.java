package org.forif_backend.domain.staff;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;

@RequiredArgsConstructor
@Getter
public enum StaffRole {

    /** 기존 tb_staff_account 행을 읽기 위한 호환 값. 새 계정과 토큰에는 사용하지 않는다. */
    MENTOR("MENTOR", "멘토"),
    ADMIN("ADMIN", "운영진");

    private final String value;
    private final String label;

    public static  StaffRole fromValue(String value) {
        return switch (value) {
            case "ADMIN" -> ADMIN;
            default -> throw new ForifException(ErrorCode.INVALID_STATUS_VALUE);
        };
    }
}
