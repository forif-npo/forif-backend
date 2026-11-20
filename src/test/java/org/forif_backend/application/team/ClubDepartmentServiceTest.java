package org.forif_backend.application.team;

import org.forif_backend.domain.team.ClubDepartment;
import org.forif_backend.domain.team.ClubDepartmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClubDepartmentServiceTest {

    @Mock private ClubDepartmentRepository repository;
    @InjectMocks private ClubDepartmentService service;

    @Test
    void 없는_부서는_입력한_이름으로_만든다() {
        when(repository.findByName("SW팀")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ClubDepartment department = service.getOrCreate(" SW팀 ");

        assertThat(department.getName()).isEqualTo("SW팀");
        verify(repository).findByName("SW팀");
        verify(repository).save(department);
    }

    @Test
    void 이미_있는_부서는_같은_행을_사용한다() {
        ClubDepartment existing = ClubDepartment.create("기획팀");
        when(repository.findByName("기획팀")).thenReturn(Optional.of(existing));

        assertThat(service.getOrCreate("기획팀")).isSameAs(existing);
        verify(repository).findByName("기획팀");
        verifyNoMoreInteractions(repository);
    }
}
