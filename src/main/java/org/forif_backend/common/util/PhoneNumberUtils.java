package org.forif_backend.common.util;

import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;

/** 전화번호의 저장·비교에 사용하는 숫자형 표준 유틸리티. */
public final class PhoneNumberUtils {

    private PhoneNumberUtils() {
    }

    /**
     * 전화번호를 숫자만 있는 형식으로 정규화한다.
     * 예: 010-1234-5678, +82 10-1234-5678 -> 01012345678
     */
    public static String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            throw new ForifException(ErrorCode.INVALID_PHONE_NUMBER);
        }

        String digits = phoneNumber.replaceAll("\\D", "");
        if (digits.startsWith("0082")) {
            digits = "0" + digits.substring(4);
        } else if (digits.startsWith("82")) {
            digits = "0" + digits.substring(2);
        }

        if (digits.isEmpty()) {
            throw new ForifException(ErrorCode.INVALID_PHONE_NUMBER);
        }
        return digits;
    }
}
