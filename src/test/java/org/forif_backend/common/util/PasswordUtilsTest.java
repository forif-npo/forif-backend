package org.forif_backend.common.util;

import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordUtilsTest {

    @Test
    void acceptsPasswordWithAtLeastTwoCharacterCategories() {
        assertThatCode(() -> PasswordUtils.validate("Password1!"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsPasswordOutsideTheSharedPolicy() {
        assertThatThrownBy(() -> PasswordUtils.validate("onlylowercase"))
                .isInstanceOf(ForifException.class)
                .extracting(exception -> ((ForifException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void onlyCountsTheSameAsciiCharacterCategoriesAsTheFrontendPolicy() {
        assertThatThrownBy(() -> PasswordUtils.validate("가나다라마바사123"))
                .isInstanceOf(ForifException.class)
                .extracting(exception -> ((ForifException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }
}
