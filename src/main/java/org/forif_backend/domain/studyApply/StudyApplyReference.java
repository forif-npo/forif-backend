package org.forif_backend.domain.studyApply;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tb_study_apply_reference")
public class StudyApplyReference {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "study_apply_reference_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apply_id", nullable = false)
    private StudyApply studyApply;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ReferenceType referenceType;

    @Column(nullable = false)
    private String content;

    private StudyApplyReference(StudyApply studyApply, ReferenceType referenceType, String content) {
        this.studyApply = studyApply;
        this.referenceType = referenceType;
        this.content = content;
    }

    public static StudyApplyReference create(StudyApply studyApply, ReferenceType type, String content) {
        return new StudyApplyReference(
                studyApply,
                type,
                content
        );
    }
}
