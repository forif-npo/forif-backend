package org.forif_backend.application.user;

import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceMemberInfoTest {

    private static final Long USER_ID = 20260001L;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void updatesOnlyDepartmentAndPhoneNumber() {
        User member = User.createUser(
                USER_ID,
                "부원",
                "member@hanyang.ac.kr",
                "010-1111-2222",
                "컴퓨터학부"
        );
        member.updateProfile(null, "users/profiles/member.png");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(member));

        userService.updateMemberInfo(USER_ID, "소프트웨어학부", "010-3333-4444");

        assertThat(member.getId()).isEqualTo(USER_ID);
        assertThat(member.getUserName()).isEqualTo("부원");
        assertThat(member.getDepartment()).isEqualTo("소프트웨어학부");
        assertThat(member.getPhoneNum()).isEqualTo("010-3333-4444");
        assertThat(member.getImgUrl()).isEqualTo("users/profiles/member.png");
    }
}
