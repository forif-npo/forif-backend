package org.forif_backend.domain.user;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum UserApplyStatus {
    PENDING("대기중"),
    ACCEPT("승낙"),
    REJECT("거절");

    private final String statusName;
}
