package org.forif_backend.domain.study;

import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

// 다중 PK를 위한 ID 클래스
@NoArgsConstructor
@EqualsAndHashCode
public class StudyUserId implements Serializable {
    private Integer study; // StudyUser 엔티티의 study 필드명과 일치
    private Long user;   // StudyUser 엔티티의 user 필드명과 일치
}