package org.forif_backend.domain.study;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.forif_backend.domain.user.User;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tb_study_user")
@IdClass(StudyUserId.class)
public class StudyUser {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_id", nullable = false)
    private Study study;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private Integer certificateStatus;

    /**
     * 수료증 파일의 S3 object key. 기존에 저장된 Presigned URL도 하위 호환을 위해 읽을 수 있다.
     */
    @Column(name = "certificate_url", length = 300)
    private String certificateObjectKey;

    public static StudyUser create(Study study, User user) {
        StudyUser studyUser = new StudyUser();
        studyUser.study = study;
        studyUser.user = user;
        return studyUser;
    }

    /**
     * 수료증 발급 처리 (0: 미발급, 1: 발급)
     */
    public void issueCertificate(String certificateObjectKey) {
        this.certificateStatus = 1;
        this.certificateObjectKey = certificateObjectKey;
    }
}
