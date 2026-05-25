package org.forif_backend.domain.hackathon;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;

@Getter
@RequiredArgsConstructor
public enum HackathonStatus {
    RECRUITING("RECRUITING"),
    TEAM_BUILDING("TEAM_BUILDING"),
    IN_PROGRESS("IN_PROGRESS"),
    JUDGING("JUDGING"),
    ENDED("ENDED");

    private final String value;

    public static HackathonStatus fromValue(String value) {
        if (value == null) {
            return null;
        }

        for (HackathonStatus status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }

        throw new ForifException(ErrorCode.INVALID_STATUS_VALUE);
    }
}
