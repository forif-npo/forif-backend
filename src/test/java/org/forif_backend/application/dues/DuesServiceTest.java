package org.forif_backend.application.dues;

import org.forif_backend.application.dues.dto.DuesPageResult;
import org.forif_backend.application.dues.dto.DuesSort;
import org.forif_backend.application.dues.dto.UpdateDuesCommand;
import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.semester.dto.SemesterInfo;
import org.forif_backend.domain.dues.MemberSemesterCheck;
import org.forif_backend.domain.dues.MemberSemesterCheckRepository;
import org.forif_backend.domain.study.StudyRepository;
import org.forif_backend.domain.study.StudyUserRepository;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DuesServiceTest {

    private static final SemesterInfo CURRENT_SEMESTER = SemesterInfo.of(2026, 2);

    @Mock
    private SemesterService semesterService;
    @Mock
    private StudyUserRepository studyUserRepository;
    @Mock
    private StudyRepository studyRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MemberSemesterCheckRepository memberSemesterCheckRepository;

    @InjectMocks
    private DuesService duesService;

    private User duesUnpaidUser;
    private User completedUser;

    @BeforeEach
    void setUp() {
        duesUnpaidUser = User.createUser(1L, "가나다", "first@hanyang.ac.kr", "01011112222", "컴퓨터학부");
        completedUser = User.createUser(2L, "나다라마바사", "second@hanyang.ac.kr", "01033334444", "전자공학부");

        when(semesterService.getActive()).thenReturn(CURRENT_SEMESTER);
        when(studyRepository.findCurrentStudyNamesByUserIds(anyList(), anyInt(), anyInt()))
                .thenReturn(Map.of(1L, "웹 스터디", 2L, "백엔드 스터디"));
    }

    @Test
    @DisplayName("상태가 없는 현재 학기 부원은 미납·미제출로 조회하고 확인 필요 순서로 정렬한다")
    void getsCurrentSemesterDuesWithDefaultUncheckedStatus() {
        MemberSemesterCheck completedCheck = MemberSemesterCheck.create(completedUser, 2026, 2);
        completedCheck.update(true, true);

        when(studyUserRepository.findUsersByYearSemester(2026, 2, null))
                .thenReturn(List.of(completedUser, duesUnpaidUser));
        when(memberSemesterCheckRepository.findAllByYearSemesterAndUserIds(2026, 2, List.of(2L, 1L)))
                .thenReturn(List.of(completedCheck));

        DuesPageResult result = duesService.getCurrentSemesterDues(0, 20, null, DuesSort.NEEDS_ATTENTION);

        assertThat(result.content()).extracting(member -> member.userId())
                .containsExactly(1L, 2L);
        assertThat(result.content().get(0).duesPaid()).isFalse();
        assertThat(result.content().get(0).googleFormSubmitted()).isFalse();
        assertThat(result.summary().totalCount()).isEqualTo(2);
        assertThat(result.summary().completedCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("체크 상태를 처음 수정하면 현재 학기 상태 행을 생성한다")
    void createsMemberCheckWhenUpdatingForTheFirstTime() {
        when(studyUserRepository.existsByUserIdAndStudyYearSemester(1L, 2026, 2)).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(duesUnpaidUser));
        when(memberSemesterCheckRepository.findByUserIdAndYearSemester(1L, 2026, 2))
                .thenReturn(Optional.empty());
        when(memberSemesterCheckRepository.save(any(MemberSemesterCheck.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = duesService.updateCurrentSemesterDues(
                1L,
                new UpdateDuesCommand(true, false)
        );

        ArgumentCaptor<MemberSemesterCheck> captor = ArgumentCaptor.forClass(MemberSemesterCheck.class);
        verify(memberSemesterCheckRepository).save(captor.capture());
        assertThat(captor.getValue().isDuesPaid()).isTrue();
        assertThat(captor.getValue().isGoogleFormSubmitted()).isFalse();
        assertThat(result.duesPaid()).isTrue();
        assertThat(result.googleFormSubmitted()).isFalse();
    }
}
