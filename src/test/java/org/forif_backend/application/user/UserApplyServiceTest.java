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
        UserApplyRequest userApplyRequest = new UserApplyRequest(1,
                "1지망",
                2,
                "2지망");
        userApplyService.applyStudy(user.getId(), userApplyRequest);

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
        UserApplyRequest userApplyRequest = new UserApplyRequest(1,
                "1지망",
                2,
                "2지망");
        userApplyService.applyStudy(user.getId(), userApplyRequest);

        // then
        // 두번 지원하면 실패
        Assertions.assertThatThrownBy(() -> {
            userApplyService.applyStudy(user.getId(), userApplyRequest);
        }).hasMessage(ErrorCode.USER_APPLY_ALREADY_EXISTS.getMessage());
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
        // 스터디 지원 내역 조회
        List<UserApplyInfo> applyInfo = userApplyService.getApplyInfo(userId, studyId, 0, 20, null, SortDirection.ASC).getContent();

        // then
        // 검증1: 지원 내역 조회 확인
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
        // 스터디 지원 내역 조회
        List<UserApplyInfo> applyInfo = userApplyService.getApplyInfo(userId, studyId, 0, 20, null, SortDirection.DESC).getContent();

        // then
        // 검증1: 지원 내역 조회 확인
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
        // 스터디 지원 내역 조회
        List<UserApplyInfo> applyInfo = userApplyService.getApplyInfo(userId, studyId, 0, 20, UserApplyStatus.ACCEPT, SortDirection.DESC).getContent();

        // then
        // 검증1: 지원 내역 조회 확인
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
        // 스터디 지원 내역 조회
        assertThatThrownBy(() -> {
            userApplyService.getApplyInfo(mentorId, studyId, 0, 20, null, SortDirection.DESC); // 해당 스터디의 멘토가 아니면 오류 발생
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
        Long mentorId = 3L; // Study 2의 Primary Mentor (김동현)
        int studyId = 2;

        // when
        // 최신순 조회 (DESC), Page 0, Size 3
        // 예상 순서: User 56 -> 55 -> 54 -> 53 -> 52 -> 51 -> 50
        List<UserApplyInfo> applyInfo = userApplyService.getApplyInfo(mentorId, studyId, 0, 3, null, SortDirection.DESC).getContent();

        // then
        assertThat(applyInfo.size()).isEqualTo(3);
        // 첫 번째 페이지의 첫 요소는 가장 최근에 신청한 User 56이어야 함
        assertThat(applyInfo.get(0).applierName()).isEqualTo("전현우"); // User 56의 이름
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
        // 최신순 조회 (DESC), Page 1, Size 3 (Offset 3)
        // 전체 7개 중 4, 5, 6번째 항목 (User 53, 52, 51)
        List<UserApplyInfo> applyInfo = userApplyService.getApplyInfo(mentorId, studyId, 1, 3, null, SortDirection.DESC).getContent();

        // then
        assertThat(applyInfo.size()).isEqualTo(3);
        // 첫 번째 페이지의 첫 요소는 User 53이어야 함
        assertThat(applyInfo.get(0).applierName()).isEqualTo("서민지"); // User 53의 이름
    }
}
