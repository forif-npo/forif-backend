package org.forif_backend.common.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.forif_backend.domain.study.RecruitStatus;

/**
 * RecruitStatus Enum을 데이터베이스의 VARCHAR 타입과 변환하기 위한 컨버터
 * @Converter(autoApply = true): 이 컨버터를 모든 RecruitStatus 타입 필드에 자동으로 적용합니다.
 */
@Converter(autoApply = true)
public class RecruitStatusConverter implements AttributeConverter<RecruitStatus, String> {

    /**
     * RecruitStatus Enum을 DB에 저장될 문자열(value)로 변환합니다.
     * @param recruitStatus 변환할 Enum 객체
     * @return DB에 저장될 문자열 (e.g., "applicable", "closed")
     */
    @Override
    public String convertToDatabaseColumn(RecruitStatus recruitStatus) {
        if (recruitStatus == null) {
            return null;
        }
        return recruitStatus.getValue();
    }

    /**
     * DB에서 읽어온 문자열(value)을 RecruitStatus Enum으로 변환합니다.
     * @param value DB에서 읽어온 문자열 (e.g., "applicable", "closed")
     * @return 변환된 Enum 객체
     */
    @Override
    public RecruitStatus convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }
        return RecruitStatus.fromValue(value);
    }
}
