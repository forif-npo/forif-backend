package org.forif_backend.domain.study;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;

@Getter
@RequiredArgsConstructor
public enum RecruitStatus {
    APPLICABLE("APPLICABLE"),
    CLOSED("CLOSED");
    
    private final String value;
    
    public static RecruitStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        
        for (RecruitStatus status : RecruitStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        
        throw new ForifException(ErrorCode.INTERNAL_SERVER_ERROR, 
            "해당하는 RecruitStatus가 없습니다. value: " + value);
    }
}
