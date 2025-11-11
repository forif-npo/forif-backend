package org.forif_backend.application.study;

import org.assertj.core.api.Assertions;
import org.forif_backend.application.studyApply.dto.CreateStudyApplyInfo;
import org.forif_backend.application.studyApply.dto.StudyApplyService;
import org.forif_backend.domain.studyApply.ReferenceType;
import org.forif_backend.domain.studyApply.StudyApply;
import org.forif_backend.domain.studyApply.StudyApplyPlan;
import org.forif_backend.domain.studyApply.StudyApplyReference;
import org.forif_backend.domain.user.User;
import org.forif_backend.infrastructure.persistence.studyApply.StudyApplyJpaRepository;
import org.forif_backend.infrastructure.persistence.studyApply.StudyApplyPlanJpaRepository;
import org.forif_backend.infrastructure.persistence.studyApply.StudyApplyReferenceJpaRepository;
import org.forif_backend.infrastructure.persistence.user.UserJpaRepository;
import org.forif_backend.mock.DefaultMockitoTest;
import org.forif_backend.web.studyApply.dto.CreateStudyApplyRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.web.multipart.MultipartFile;
import java.time.ZonedDateTime;
import java.util.List;

public class StudyServiceTest extends DefaultMockitoTest {
    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private StudyApplyService studyApplyService;

    @Autowired
    private StudyApplyJpaRepository studyApplyJpaRepository;

    @Autowired
    private StudyApplyPlanJpaRepository studyApplyPlanJpaRepository;

    @Autowired
    private StudyApplyReferenceJpaRepository studyApplyReferenceJpaRepository;

    @Test
    @DisplayName("스터디 신청 테스트")
    @Sql(statements = {
            "ALTER TABLE tb_user ALTER COLUMN user_id RESTART WITH 1",
            "ALTER TABLE tb_study ALTER COLUMN study_id RESTART WITH 1"
    })
    @Sql({"/sql/user-test-data.sql", "/sql/study-test-data.sql"})
    void study_apply_test_success() {
        // given
        // 테스트용 사용자 조회
        User user = userJpaRepository.findById(1L).get();

        // when
        // 스터디 지원
        MultipartFile thumbnail = new MockMultipartFile(
                "file",
                "thumbnail.jpeg",
                "image/jpeg",
                "thumbnail".getBytes()
        );

        MultipartFile reference = new MockMultipartFile(
                "file",
                "reference.jpeg",
                "image/jpeg",
                "reference".getBytes()
        );

        CreateStudyApplyRequest createStudyApplyRequest = new CreateStudyApplyRequest(
                "포리프 웹 만들기",
                "웹 만들기 스터디입니다.",
                List.of(3L, 4L),
                "포리프 웹 완성이 목표입니다.",
                "프론트, 백이 소통하며 웹을 만듭니다.",
                false,
                "포리프 동방",
                "한양대학교 대운동장",
                2,
                "18:00",
                "20:00",
                List.of(new CreateStudyApplyRequest.Plan(1, ZonedDateTime.now(), "기획", "기획하기"), new CreateStudyApplyRequest.Plan(2, ZonedDateTime.now(), "개발", "개발하기")),
                3,
                "잘하는 순으로 뽑습니다.",
                6,
                true,
                ZonedDateTime.now(),
                List.of(new CreateStudyApplyRequest.Reference(ReferenceType.FILE, null, reference.getOriginalFilename()), new CreateStudyApplyRequest.Reference(ReferenceType.URL, "https://forif.com", null))
        );

        CreateStudyApplyInfo studyApplyResponse = studyApplyService.createStudyApply(user.getId(), createStudyApplyRequest, thumbnail, List.of(reference));

        //then
        StudyApply findStudyApply = studyApplyJpaRepository.findByPrimaryMentor(user).get();
        List<StudyApplyPlan> findStudyApplyPlan = studyApplyPlanJpaRepository.findByStudyApply(findStudyApply);
        List<StudyApplyReference> findStudyReference = studyApplyReferenceJpaRepository.findByStudyApply(findStudyApply);

        // 검증1: 스터디 신청 내용 검증
        Assertions.assertThat(findStudyApply.getStudyName()).isEqualTo("포리프 웹 만들기");
        Assertions.assertThat(findStudyApply.getCapacity()).isEqualTo(6);
        Assertions.assertThat(findStudyApply.getGoal()).isEqualTo("포리프 웹 완성이 목표입니다.");

        // 검증2: 스터디 계획 내용 검증
        Assertions.assertThat(findStudyApplyPlan.size()).isEqualTo(2);
        Assertions.assertThat(findStudyApplyPlan.get(0).getContent()).isEqualTo("기획하기");
        Assertions.assertThat(findStudyApplyPlan.get(0).getWeekNum()).isEqualTo(1);

        // 검증3: 신청 참고자료 내용 검증
        Assertions.assertThat(findStudyReference.size()).isEqualTo(2);
        Assertions.assertThat(findStudyReference.get(0).getReferenceType()).isEqualTo(ReferenceType.FILE);
        Assertions.assertThat(findStudyReference.get(0).getContent().contains(reference.getOriginalFilename())).isTrue();

        // 검증4: response 내용 검증
        Assertions.assertThat(studyApplyResponse.thumbnailUploadInfo().objectKey().contains(thumbnail.getOriginalFilename())).isTrue();
        Assertions.assertThat(studyApplyResponse.referenceUploadInfos().size()).isEqualTo(1);
        Assertions.assertThat(studyApplyResponse.referenceUploadInfos().get(0).objectKey().contains(reference.getOriginalFilename())).isTrue();
    }
}
