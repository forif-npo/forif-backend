package org.forif_backend.common.util;

import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhoneNumberUtilsTest {

    @Test
    void normalizesDomesticAndInternationalKoreanPhoneNumbers() {
        assertThat(PhoneNumberUtils.normalizePhoneNumber("010-1234-5678"))
                .isEqualTo("01012345678");
        assertThat(PhoneNumberUtils.normalizePhoneNumber("+82 10-1234-5678"))
                .isEqualTo("01012345678");
        assertThat(PhoneNumberUtils.normalizePhoneNumber("02-1234-5678"))
                .isEqualTo("0212345678");
    }

    @Test
    void rejectsValuesWithoutDigits() {
        assertThatThrownBy(() -> PhoneNumberUtils.normalizePhoneNumber("---"))
                .isInstanceOf(ForifException.class)
                .extracting(exception -> ((ForifException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PHONE_NUMBER);
    }
}
