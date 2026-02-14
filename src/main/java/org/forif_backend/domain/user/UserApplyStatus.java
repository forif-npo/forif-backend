package org.forif_backend.domain.user;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum UserApplyStatus {
    PENDING("대기중"),
    ACCEPT("승낙"),
    REJECT("거절"),
    WAITLIST("예비");

    private final String statusName;
}
