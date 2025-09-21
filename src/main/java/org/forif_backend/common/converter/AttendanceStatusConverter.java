package org.forif_backend.common.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.forif_backend.domain.study.AttendanceStatus;

@Converter(autoApply = true)
public class AttendanceStatusConverter implements AttributeConverter<AttendanceStatus, String> {

    @Override
    public String convertToDatabaseColumn(AttendanceStatus status) {
        if (status == null) return null;
        return status.getValue();
    }

    @Override
    public AttendanceStatus convertToEntityAttribute(String value) {
        if (value == null) return null;
        return AttendanceStatus.fromValue(value);
    }
}
