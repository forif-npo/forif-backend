package org.forif_backend.domain.staff;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;

@RequiredArgsConstructor
@Getter
public enum StaffRole {

    MENTOR("MENTOR", "멘토"),
    ADMIN("ADMIN", "운영진");

    private final String value;
    private final String label;

    public static  StaffRole fromValue(String value) {
        return switch (value) {
            case "MENTOR" -> MENTOR;
            case "ADMIN" -> ADMIN;
            default -> throw new ForifException(ErrorCode.INTERNAL_SERVER_ERROR);
        };
    }
}
