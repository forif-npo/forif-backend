package org.forif_backend.infrastructure.persistence.staffrecruitment;

import jakarta.persistence.EntityManager;
import org.forif_backend.common.config.JpaAuditingConfig;
import org.forif_backend.domain.staffrecruitment.StaffApplication;
import org.forif_backend.domain.staffrecruitment.StaffApplicationAnswer;
import org.forif_backend.domain.staffrecruitment.StaffApplicationChoice;
import org.forif_backend.domain.staffrecruitment.StaffRecruitment;
import org.forif_backend.domain.staffrecruitment.StaffRecruitmentDepartmentSpec;
import org.forif_backend.domain.staffrecruitment.StaffRecruitmentQuestionSpec;
import org.forif_backend.domain.staffrecruitment.StaffRecruitmentQuestionType;
import org.forif_backend.domain.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class StaffApplicationJpaRepositoryHistoryTest {

    @Autowired private EntityManager entityManager;
    @Autowired private StaffApplicationJpaRepository repository;

    @Test
    void 본인_지원서만_모든_회차에서_최신순으로_조회한다() {
        User applicant = persistUser(101L);
        User other = persistUser(202L);
        StaffRecruitment past = persistRecruitment(2026, 1, LocalDateTime.of(2026, 5, 1, 12, 0));
        StaffRecruitment recent = persistRecruitment(2026, 2, LocalDateTime.of(2026, 11, 1, 12, 0));
        past.publish();
        recent.publish();
        StaffApplication pastMine = persistApplication(past, applicant, LocalDateTime.of(2026, 5, 1, 12, 0));
        StaffApplication recentMine = persistApplication(recent, applicant, LocalDateTime.of(2026, 11, 1, 12, 0));
        persistApplication(recent, other, LocalDateTime.of(2026, 11, 1, 13, 0));
        entityManager.flush();
        entityManager.clear();

        var history = repository.findByApplicantId(applicant.getId());

        assertThat(history).extracting(StaffApplication::getId)
                .containsExactly(recentMine.getId(), pastMine.getId());
        assertThat(history).allSatisfy(application ->
                assertThat(application.getRecruitment().getActYear()).isEqualTo(2026));
    }

    @Test
    void 지망과_답변을_교체한_뒤_지원서를_삭제할_수_있다() {
        User applicant = persistUser(303L);
        StaffRecruitment recruitment = persistRecruitment(2027, 1, LocalDateTime.now());
        recruitment.mergeDepartments(List.of(
                new StaffRecruitmentDepartmentSpec(null, "기획팀", null, 1, true),
                new StaffRecruitmentDepartmentSpec(null, "개발팀", null, 2, true)), false);
        recruitment.mergeQuestions(List.of(new StaffRecruitmentQuestionSpec(
                null, "지원 동기", null, StaffRecruitmentQuestionType.LONG_TEXT, true, 100, 1)), false);
        recruitment.publish();
        entityManager.flush();

        StaffApplication application = persistApplication(recruitment, applicant, LocalDateTime.now());
        application.addChoice(StaffApplicationChoice.create(recruitment.getDepartments().get(0), 1));
        application.addAnswer(StaffApplicationAnswer.create(recruitment.getQuestions().get(0), "기존 답변"));
        entityManager.flush();
        Integer applicationId = application.getId();
        entityManager.clear();

        StaffApplication managed = repository.findForMutation(
                recruitment.getId(), applicant.getId()).orElseThrow();
        managed.clearSubmission();
        repository.flush();
        managed.addChoice(StaffApplicationChoice.create(managed.getRecruitment().getDepartments().get(1), 1));
        managed.addAnswer(StaffApplicationAnswer.create(managed.getRecruitment().getQuestions().get(0), "수정 답변"));
        repository.flush();
        entityManager.clear();

        StaffApplication updated = repository.findDetailById(applicationId).orElseThrow();
        assertThat(updated.getChoices()).extracting(choice -> choice.getDepartment().getName())
                .containsExactly("개발팀");
        assertThat(updated.getAnswers()).extracting(StaffApplicationAnswer::getAnswer)
                .containsExactly("수정 답변");

        repository.delete(updated);
        repository.flush();
        entityManager.clear();
        assertThat(repository.findById(applicationId)).isEmpty();

        User sameApplicant = entityManager.getReference(User.class, applicant.getId());
        StaffRecruitment sameRecruitment = entityManager.getReference(StaffRecruitment.class, recruitment.getId());
        StaffApplication reapplied = persistApplication(sameRecruitment, sameApplicant, LocalDateTime.now());
        repository.flush();
        assertThat(reapplied.getId()).isNotEqualTo(applicationId);
        assertThat(repository.existsByRecruitmentIdAndApplicantId(recruitment.getId(), applicant.getId())).isTrue();
    }

    private User persistUser(Long userId) {
        User user = User.createUser(userId, "테스트 회원", userId + "@example.com", "01000000000", "예시 학과");
        entityManager.persist(user);
        return user;
    }

    private StaffRecruitment persistRecruitment(int year, int semester, LocalDateTime submittedAt) {
        StaffRecruitment recruitment = StaffRecruitment.create(year, semester,
                year + "-" + semester, null, "모집 안내", submittedAt.minusDays(1), submittedAt.plusDays(1));
        entityManager.persist(recruitment);
        return recruitment;
    }

    private StaffApplication persistApplication(StaffRecruitment recruitment, User applicant, LocalDateTime submittedAt) {
        StaffApplication application = StaffApplication.submit(recruitment, applicant, applicant.getUserName(),
                applicant.getEmail(), applicant.getPhoneNum(), applicant.getDepartment(), submittedAt);
        entityManager.persist(application);
        return application;
    }
}
