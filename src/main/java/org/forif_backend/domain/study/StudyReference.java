package org.forif_backend.domain.study;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tb_study_reference")
public class StudyReference {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "study_reference_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_id", nullable = false)
    private Study study;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ReferenceType referenceType;

    @Column(nullable = false)
    private String content;

    private StudyReference(Study study, ReferenceType referenceType, String content) {
        this.study = study;
        this.referenceType = referenceType;
        this.content = content;
    }

    public static StudyReference create(Study study, ReferenceType type, String content) {
        return new StudyReference(
                study,
                type,
                content
        );
    }
}
