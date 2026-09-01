package org.forif_backend.domain.study;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;

@Getter
@RequiredArgsConstructor
public enum StudyDifficulty {
    EASY("EASY", 1),
    SEMI_EASY("SEMI_EASY", 2),
    NORMAL("NORMAL", 3),
    SEMI_HARD("SEMI_HARD", 4),
    HARD("HARD", 5);

    private final String value;
    private final Integer level;

    public static StudyDifficulty fromValue(String value) {
        if (value == null) {
            return null;
        }

        for (StudyDifficulty difficulty : StudyDifficulty.values()) {
            if (difficulty.value.equalsIgnoreCase(value)) {
                return difficulty;
            }
        }

        throw new ForifException(ErrorCode.INVALID_STATUS_VALUE);
    }

    public static StudyDifficulty fromLevel(Integer level) {
        if (level == null) {
            return null;
        }

        for (StudyDifficulty difficulty : StudyDifficulty.values()) {
            if (difficulty.level.equals(level)) {
                return difficulty;
            }
        }

        throw new ForifException(ErrorCode.INVALID_STATUS_VALUE);
    }
}
