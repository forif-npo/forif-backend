package org.forif_backend.common.converter;

import org.forif_backend.domain.study.StudyDifficulty;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class StudyDifficultyConverter implements AttributeConverter<StudyDifficulty, Integer> {
    @Override
    public Integer convertToDatabaseColumn(StudyDifficulty difficulty) {
        return difficulty != null ? difficulty.getLevel() : null;
    }
    
    @Override
    public StudyDifficulty convertToEntityAttribute(Integer level) {
        return StudyDifficulty.fromLevel(level);
    }
}