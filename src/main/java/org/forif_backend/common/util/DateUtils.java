package org.forif_backend.common.util;

import lombok.experimental.UtilityClass;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@UtilityClass
public class DateUtils {
    public static ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");

    public static int getCurrentYear() {
        return ZonedDateTime.now(ZONE_SEOUL).getYear();
    }

    public static int getCurrentSemester() {
        // 7월까지 1학기 8월부터 2학기
        return ZonedDateTime.now(ZONE_SEOUL).getMonthValue() <= 7 ? 1 : 2;
    }
}
