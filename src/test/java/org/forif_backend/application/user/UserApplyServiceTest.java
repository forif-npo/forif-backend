package org.forif_backend.application.user;

import org.assertj.core.api.Assertions;
import org.forif_backend.application.user.dto.UserApplyInfo;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.type.SortDirection;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserApply;
import org.forif_backend.domain.user.UserApplyStatus;
import org.forif_backend.infrastructure.persistence.user.UserApplyJpaRepository;
import org.forif_backend.infrastructure.persistence.user.UserJpaRepository;
import org.forif_backend.mock.DefaultMockitoTest;
import org.forif_backend.web.userApply.dto.UserApplyRequest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Disabled
public class UserApplyServiceTest extends DefaultMockitoTest {
    @Autowired
    UserApplyService userApplyService;

    @Autowired
    UserApplyJpaRepository userApplyJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Test
    @DisplayName("스터디 지원 테스트: 사용자가 1순위 스터디에 정상적으로 지원한다.")
    @Sql(statements = {
            "ALTER TABLE tb_user ALTER COLUMN user_id RESTART WITH 1",
            "ALTER TABLE tb_study ALTER COLUMN study_id RESTART WITH 1"
    })
    @Sql({"/sql/user-test-data.sql", "/sql/study-test-data.sql"})
    void apply_test_primary_success() {
        // given
        User user = userJpaRepository.findById(1L).get();

        // when
        UserApplyRequest request = new UserApplyRequest(1, "1순위 스터디에 지원하는 이유는 웹 개발의 기초부터 심화까지 배우고 싶기 때문입니다.", 1);
        userApplyService.applyStudy(user.getId(), request);

        // then
        List<UserApply> userApply = userApplyJpaRepository.findByApplier(user);
        assertThat(userApply.size()).isEqualTo(1);
        UserApply applyData = userApply.get(0);
        assertThat(applyData.getPrimaryStudy()).isEqualTo(1);
        assertThat(applyData.getSecondaryStudy()).isNull();
    }

    @Test
    @DisplayName("스터디 지원 테스트: 1순위 지원 후 2순위 스터디에 정상적으로 지원한다.")
    @Sql(statements = {
            "ALTER TABLE tb_user ALTER COLUMN user_id RESTART WITH 1",
            "ALTER TABLE tb_study ALTER COLUMN study_id RESTART WITH 1"
    })
    @Sql({"/sql/user-test-data.sql", "/sql/study-test-data.sql"})
    void apply_test_secondary_success() {
        // given
        User user = userJpaRepository.findById(1L).get();

        // when - 1순위 지원
        UserApplyRequest primaryRequest = new UserApplyRequest(1, "1순위 스터디에 지원하는 이유는 웹 개발의 기초부터 심화까지 배우고 싶기 때문입니다.", 1);
        userApplyService.applyStudy(user.getId(), primaryRequest);

        // when - 2순위 지원
        UserApplyRequest secondaryRequest = new UserApplyRequest(2, "2순위 스터디에 지원하는 이유는 백엔드 개발 역량을 키우기 위해서입니다.", 2);
        userApplyService.applyStudy(user.getId(), secondaryRequest);

        // then
        List<UserApply> userApply = userApplyJpaRepository.findByApplier(user);
        assertThat(userApply.size()).isEqualTo(1);
        UserApply applyData = userApply.get(0);
        assertThat(applyData.getPrimaryStudy()).isEqualTo(1);
        assertThat(applyData.getSecondaryStudy()).isEqualTo(2);
    }

    @Test
    @DisplayName("스터디 지원 테스트: 이미 1순위를 지원했는데 다시 1순위로 지원하면 실패")
    @Sql(statements = {
            "ALTER TABLE tb_user ALTER COLUMN user_id RESTART WITH 1",
            "ALTER TABLE tb_study ALTER COLUMN study_id RESTART WITH 1"
    })
    @Sql({"/sql/user-test-data.sql", "/sql/study-test-data.sql"})
    void apply_test_duplicate_primary_fail() {
        // given
        User user = userJpaRepository.findById(1L).get();

        // when
        UserApplyRequest request = new UserApplyRequest(1, "1순위 스터디에 지원하는 이유는 웹 개발의 기초부터 심화까지 배우고 싶기 때문입니다.", 1);
        userApplyService.applyStudy(user.getId(), request);

        // then
        Assertions.assertThatThrownBy(() -> {
            userApplyService.applyStudy(user.getId(), request);
        }).hasMessage(ErrorCode.ALREADY_APPLIED_PRIMARY.getMessage());
    }

    @Test
    @DisplayName("스터디 지원 테스트: 1순위 없이 2순위 지원하면 실패")
    @Sql(statements = {
            "ALTER TABLE tb_user ALTER COLUMN user_id RESTART WITH 1",
            "ALTER TABLE tb_study ALTER COLUMN study_id RESTART WITH 1"
    })
    @Sql({"/sql/user-test-data.sql", "/sql/study-test-data.sql"})
    void apply_test_secondary_without_primary_fail() {
        // given
        User user = userJpaRepository.findById(1L).get();

        // when & then
        UserApplyRequest request = new UserApplyRequest(2, "2순위 스터디에 지원하는 이유는 백엔드 개발 역량을 키우기 위해서입니다.", 2);
        assertThatThrownBy(() -> {
            userApplyService.applyStudy(user.getId(), request);
        }).hasMessage(ErrorCode.PRIMARY_NOT_APPLIED.getMessage());
    }

    @Test
    @DisplayName("스터디 지원 내역 조회 테스트: 지원내역을 정상적으로 불러온다.(오래된순)")
    @Sql(statements = {
            "ALTER TABLE tb_user ALTER COLUMN user_id RESTART WITH 1",
            "ALTER TABLE tb_study ALTER COLUMN study_id RESTART WITH 1"
    })
    @Sql({"/sql/user-test-data.sql", "/sql/study-test-data.sql", "/sql/user-apply-test-data.sql"})
    void apply_info_get_slower_success() {
        // given
        Long userId = 1L;
        Integer studyId = 1;

        // when
        List<UserApplyInfo> applyInfo = userApplyService.getApplyInfo(userId, studyId, 0, 20, null, SortDirection.ASC).getContent();

        // then
        assertThat(applyInfo.size()).isEqualTo(2);
        assertThat(applyInfo.get(0).applyDate()).isBefore(applyInfo.get(1).applyDate());
    }

    @Test
    @DisplayName("스터디 지원 내역 조회 테스트: 지원내역을 정상적으로 불러온다.(최신순)")
    @Sql(statements = {
            "ALTER TABLE tb_user ALTER COLUMN user_id RESTART WITH 1",
            "ALTER TABLE tb_study ALTER COLUMN study_id RESTART WITH 1"
    })
    @Sql({"/sql/user-test-data.sql", "/sql/study-test-data.sql", "/sql/user-apply-test-data.sql"})
    void apply_info_get_faster_success() {
        // given
        Long userId = 2L;
        Integer studyId = 1;

        // when
        List<UserApplyInfo> applyInfo = userApplyService.getApplyInfo(userId, studyId, 0, 20, null, SortDirection.DESC).getContent();

        // then
        assertThat(applyInfo.size()).isEqualTo(2);
        assertThat(applyInfo.get(0).applyDate()).isAfter(applyInfo.get(1).applyDate());
    }

    @Test
    @DisplayName("스터디 지원 내역 조회 테스트: 지원내역을 정상적으로 불러온다.(필터링)")
    @Sql(statements = {
            "ALTER TABLE tb_user ALTER COLUMN user_id RESTART WITH 1",
            "ALTER TABLE tb_study ALTER COLUMN study_id RESTART WITH 1"
    })
    @Sql({"/sql/user-test-data.sql", "/sql/study-test-data.sql", "/sql/user-apply-test-data.sql"})
    void apply_info_get_filter_success() {
        // given
        Long userId = 2L;
        Integer studyId = 1;

        // when
        List<UserApplyInfo> applyInfo = userApplyService.getApplyInfo(userId, studyId, 0, 20, UserApplyStatus.ACCEPT, SortDirection.DESC).getContent();

        // then
        assertThat(applyInfo.size()).isEqualTo(1);
        assertThat(applyInfo.get(0).studyStatus()).isEqualTo(UserApplyStatus.ACCEPT.getStatusName());
        assertThat(applyInfo.get(0).studyName()).isEqualTo("Forif 웹 개발 스터디");
        assertThat(applyInfo.get(0).studyComment()).isEqualTo("동아리 대표 스터디라 꼭 참여하고 싶습니다.");
    }

    @Test
    @DisplayName("스터디 지원 내역 조회 테스트: 권한 오류")
    @Sql(statements = {
            "ALTER TABLE tb_user ALTER COLUMN user_id RESTART WITH 1",
            "ALTER TABLE tb_study ALTER COLUMN study_id RESTART WITH 1"
    })
    @Sql({"/sql/user-test-data.sql", "/sql/study-test-data.sql", "/sql/user-apply-test-data.sql"})
    void apply_info_get_error() {
        // given
        Long mentorId = 3L;
        Integer studyId = 1;

        // when
        assertThatThrownBy(() -> {
            userApplyService.getApplyInfo(mentorId, studyId, 0, 20, null, SortDirection.DESC);
        }).hasMessage(ErrorCode.NOT_STUDY_MENTOR.getMessage());
    }

    @Test
    @DisplayName("스터디 지원 내역 조회 테스트: 페이징 처리가 정상적으로 동작한다. (StudyId 2, Page 0, Size 3)")
    @Sql(statements = {
            "ALTER TABLE tb_user ALTER COLUMN user_id RESTART WITH 1",
            "ALTER TABLE tb_study ALTER COLUMN study_id RESTART WITH 1"
    })
    @Sql({"/sql/user-test-data.sql", "/sql/study-test-data.sql", "/sql/user-apply-test-data.sql"})
    void apply_info_get_pagination_page0_success() {
        // given
        Long mentorId = 3L;
        int studyId = 2;

        // when
        List<UserApplyInfo> applyInfo = userApplyService.getApplyInfo(mentorId, studyId, 0, 3, null, SortDirection.DESC).getContent();

        // then
        assertThat(applyInfo.size()).isEqualTo(3);
        assertThat(applyInfo.get(0).applierName()).isEqualTo("전현우");
    }

    @Test
    @DisplayName("스터디 지원 내역 조회 테스트: 다음 페이지 조회가 정상적으로 동작한다. (StudyId 2, Page 1, Size 3)")
    @Sql(statements = {
            "ALTER TABLE tb_user ALTER COLUMN user_id RESTART WITH 1",
            "ALTER TABLE tb_study ALTER COLUMN study_id RESTART WITH 1"
    })
    @Sql({"/sql/user-test-data.sql", "/sql/study-test-data.sql", "/sql/user-apply-test-data.sql"})
    void apply_info_get_pagination_page1_success() {
        // given
        Long mentorId = 3L;
        int studyId = 2;

        // when
        List<UserApplyInfo> applyInfo = userApplyService.getApplyInfo(mentorId, studyId, 1, 3, null, SortDirection.DESC).getContent();

        // then
        assertThat(applyInfo.size()).isEqualTo(3);
        assertThat(applyInfo.get(0).applierName()).isEqualTo("서민지");
    }
}
