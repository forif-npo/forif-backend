package org.forif_backend.application.study.dto;

import org.forif_backend.domain.study.MentorStudy;
import org.forif_backend.domain.study.Study;
import org.forif_backend.domain.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StudyDetailDtoMentorsTest {

    @Test
    @DisplayName("레거시 조인 테이블에 기록이 없으면 FK 컬럼에서 멘토 목록을 만든다")
    void buildsMentorsFromForeignKeysWhenLegacyRowsAreMissing() {
        User primary = mock(User.class);
        User secondary = mock(User.class);
        when(primary.getId()).thenReturn(10L);
        when(secondary.getId()).thenReturn(20L);

        Study study = mock(Study.class);
        when(study.getPrimaryMentor()).thenReturn(primary);
        when(study.getSecondaryMentor()).thenReturn(secondary);
        when(study.getPrimaryMentorName()).thenReturn("주멘토");
        when(study.getSecondaryMentorName()).thenReturn("부멘토");

        StudyDetailDto dto = StudyDetailDto.of(study, List.of(), List.of(), List.of());

        assertThat(dto.getMentors()).hasSize(2);
        assertThat(dto.getMentors().get(0).getMentorId()).isEqualTo(10L);
        assertThat(dto.getMentors().get(0).getMentorName()).isEqualTo("주멘토");
        assertThat(dto.getMentors().get(0).getMentorNum()).isEqualTo(1);
        // 부멘토가 빠지면 클라이언트가 '없음'으로 오해하고 저장 시 실제 부멘토가 지워진다
        assertThat(dto.getMentors().get(1).getMentorId()).isEqualTo(20L);
        assertThat(dto.getMentors().get(1).getMentorName()).isEqualTo("부멘토");
        assertThat(dto.getMentors().get(1).getMentorNum()).isEqualTo(2);
    }

    @Test
    @DisplayName("부멘토가 없으면 주멘토만 담는다")
    void buildsPrimaryOnlyWhenNoSecondaryMentor() {
        User primary = mock(User.class);
        when(primary.getId()).thenReturn(10L);

        Study study = mock(Study.class);
        when(study.getPrimaryMentor()).thenReturn(primary);
        when(study.getSecondaryMentor()).thenReturn(null);
        when(study.getPrimaryMentorName()).thenReturn("주멘토");

        StudyDetailDto dto = StudyDetailDto.of(study, List.of(), List.of(), List.of());

        assertThat(dto.getMentors()).hasSize(1);
        assertThat(dto.getMentors().get(0).getMentorNum()).isEqualTo(1);
    }

    @Test
    @DisplayName("레거시 기록이 있으면 그대로 쓴다")
    void keepsLegacyMentorRowsWhenPresent() {
        User legacyMentor = mock(User.class);
        when(legacyMentor.getId()).thenReturn(99L);
        when(legacyMentor.getUserName()).thenReturn("레거시멘토");

        MentorStudy mentorStudy = mock(MentorStudy.class);
        when(mentorStudy.getMentor()).thenReturn(legacyMentor);
        when(mentorStudy.getMentorNum()).thenReturn(1);

        Study study = mock(Study.class);

        StudyDetailDto dto = StudyDetailDto.of(study, List.of(), List.of(), List.of(mentorStudy));

        assertThat(dto.getMentors()).hasSize(1);
        assertThat(dto.getMentors().get(0).getMentorId()).isEqualTo(99L);
        assertThat(dto.getMentors().get(0).getMentorName()).isEqualTo("레거시멘토");
    }
}
