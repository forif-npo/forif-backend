package org.forif_backend.infrastructure.persistence.study;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

class StudyRepositoryImplUnitTest {

    @Test
    @DisplayName("스터디 커리큘럼 삭제는 새 커리큘럼 저장 전에 반영되도록 flush한다")
    void deleteStudyPlansByStudyId_flushesAfterDelete() {
        // given
        StudyPlanJpaRepository studyPlanJpaRepository = mock(StudyPlanJpaRepository.class);
        StudyRepositoryImpl studyRepository = new StudyRepositoryImpl(
                null, null, null, studyPlanJpaRepository, null, null, null);

        // when
        studyRepository.deleteStudyPlansByStudyId(112);

        // then
        InOrder inOrder = inOrder(studyPlanJpaRepository);
        inOrder.verify(studyPlanJpaRepository).deleteByStudyId(112);
        inOrder.verify(studyPlanJpaRepository).flush();
    }
}
