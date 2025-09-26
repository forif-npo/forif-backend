package org.forif_backend.application.user;

import org.assertj.core.api.Assertions;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserApply;
import org.forif_backend.infrastructure.persistence.user.UserApplyJpaRepository;
import org.forif_backend.infrastructure.persistence.user.UserJpaRepository;
import org.forif_backend.web.user.dto.StudyApplyRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@Rollback
public class UserServiceTest {
    @Autowired
    UserService userService;

    @Autowired
    UserApplyJpaRepository userApplyJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Test
    @DisplayName("스터디 지원 테스트: 사용자가 1지망, 2지망 스터디에 정상적으로 지원한다.")
    @Sql(statements = {
            "ALTER TABLE tb_user ALTER COLUMN user_id RESTART WITH 1",
            "ALTER TABLE tb_study ALTER COLUMN study_id RESTART WITH 1"
    })
    @Sql({"/sql/user-test-data.sql", "/sql/study-test-data.sql"})
    void apply_test_success() {
        // given
        // 테스트용 사용자 조회
        User user = userJpaRepository.findById(1L).get();

        // when
        // 스터디 지원
        StudyApplyRequest studyApplyRequest = new StudyApplyRequest(1,
                "1지망",
                2,
                "2지망");
        userService.applyStudy(user, studyApplyRequest);

        // then
        List<UserApply> userApply = userApplyJpaRepository.findByApplier(user);

        // 검증1: 지원 내역 생성 확인
        assertThat(userApply.size()).isEqualTo(1);
        // 검증2: 지원 내역 상세 정보 확인
        UserApply applyData = userApply.get(0);
        assertThat(applyData.getPrimaryStudy()).isEqualTo(1);
        assertThat(applyData.getPrimaryIntro()).isEqualTo("1지망");
        assertThat(applyData.getSecondaryStudy()).isEqualTo(2);
        assertThat(applyData.getSecondaryIntro()).isEqualTo("2지망");
    }

    @Test
    @DisplayName("스터디 지원 테스트: 같은 학기에 두번 지원하면 실패")
    @Sql(statements = {
            "ALTER TABLE tb_user ALTER COLUMN user_id RESTART WITH 1",
            "ALTER TABLE tb_study ALTER COLUMN study_id RESTART WITH 1"
    })
    @Sql({"/sql/user-test-data.sql", "/sql/study-test-data.sql"})
    void apply_test_fail() {
        // given
        // 테스트용 사용자 조회
        User user = userJpaRepository.findById(1L).get();

        // when
        // 스터디 지원
        StudyApplyRequest studyApplyRequest = new StudyApplyRequest(1,
                "1지망",
                2,
                "2지망");
        userService.applyStudy(user, studyApplyRequest);

        // then
        // 두번 지원하면 실패
        Assertions.assertThatThrownBy(() -> {
            userService.applyStudy(user, studyApplyRequest);
        }).hasMessage(ErrorCode.USER_APPLY_ALREADY_EXISTS.getMessage());
    }
}
