package org.forif_backend.domain.study;

import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@EqualsAndHashCode
public class MentorStudyId implements Serializable {
    private Long mentor;  // MentorStudy 엔티티의 mentor 필드명과 일치
    private Integer study; // MentorStudy 엔티티의 study 필드명과 일치
}