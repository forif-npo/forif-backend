package org.forif_backend.common.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.forif_backend.domain.study.StudyTag;

/**
 * StudyTag Enum을 데이터베이스의 VARCHAR 타입과 변환하기 위한 컨버터
 * @Converter(autoApply = true): 이 컨버터를 모든 StudyTag 타입 필드에 자동으로 적용합니다.
 */
@Converter(autoApply = true)
public class StudyTagConverter implements AttributeConverter<StudyTag, String> {

    /**
     * StudyTag Enum을 DB에 저장될 문자열(value)로 변환합니다.
     * @param studyTag 변환할 Enum 객체
     * @return DB에 저장될 문자열 (e.g., "backend")
     */
    @Override
    public String convertToDatabaseColumn(StudyTag studyTag) {
        if (studyTag == null) {
            return null;
        }
        return studyTag.getValue();
    }

    /**
     * DB에서 읽어온 문자열(value)을 StudyTag Enum으로 변환합니다.
     * @param value DB에서 읽어온 문자열 (e.g., "backend")
     * @return 변환된 Enum 객체
     */
    @Override
    public StudyTag convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }
        return StudyTag.fromValue(value);
    }
}

