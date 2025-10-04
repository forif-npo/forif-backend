package org.forif_backend.domain.study;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;

@Getter
@RequiredArgsConstructor
public enum StudyDifficulty {
    EASY("easy", 1),
    SEMI_EASY("semi_easy", 2),
    NORMAL("normal", 3),
    SEMI_HARD("semi_hard", 4),
    HARD("hard", 5);

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

        throw new ForifException(ErrorCode.INTERNAL_SERVER_ERROR, "Invalid difficulty: " + value);
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

        throw new ForifException(ErrorCode.INTERNAL_SERVER_ERROR,
                "Invalid difficulty level: " + level);
    }
}
