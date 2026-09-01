package org.forif_backend.common.util;

import lombok.experimental.UtilityClass;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;

@UtilityClass
public class PasswordUtils {

    private static final String SPECIAL_CHARACTERS = "!@#$%^&*(),.?\":{}|<>";

    public static void validate(String password) {
        if (!isValid(password)) {
            throw new ForifException(ErrorCode.INVALID_INPUT);
        }
    }

    public static boolean isValid(String password) {
        if (password == null || password.length() < 8 || password.length() > 20) {
            return false;
        }

        int categoryCount = 0;
        if (password.matches(".*[A-Z].*")) categoryCount++;
        if (password.matches(".*[a-z].*")) categoryCount++;
        if (password.matches(".*[0-9].*")) categoryCount++;
        if (password.chars().anyMatch(ch -> SPECIAL_CHARACTERS.indexOf(ch) >= 0)) categoryCount++;
        return categoryCount >= 2;
    }
}
