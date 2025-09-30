package org.forif_backend.domain.staff;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.forif_backend.common.BaseTimeEntity;
import org.forif_backend.domain.user.User;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tb_staff_account")
public class StaffAccount extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "staff_account_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(length = 50, nullable = false, unique = true)
    private String loginId;

    @Column(length = 100, nullable = false)
    private String password;

    @Column(length = 50, nullable = false)
    private String name;

    @Column(length = 30, nullable = false)
    private String role;

    private StaffAccount(User user, String loginId, String password, String name, String role) {
        this.user = user;
        this.loginId = loginId;
        this.password = password;
        this.name = name;
        this.role = role;
    }

    public static StaffAccount createMentor(User user, String loginId, String password, String name) {
        return new StaffAccount(user, loginId, password, name, "MENTOR");
    }
}