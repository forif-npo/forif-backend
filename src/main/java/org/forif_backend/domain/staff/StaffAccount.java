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
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(length = 100, nullable = false)
    private String password;

    @Column(length = 50, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private StaffRole role;

    private StaffAccount(User user, String password, String name, StaffRole role) {
        this.user = user;
        this.password = password;
        this.name = name;
        this.role = role;
    }

    public static StaffAccount createMentor(User user, String password, String name) {
        return new StaffAccount(user, password, name, StaffRole.MENTOR);
    }

    public static StaffAccount createAdmin(User user, String password, String name) {
        return new StaffAccount(user, password, name, StaffRole.ADMIN);
    }

    /**
     * User ID 반환
     * StaffAccountService에서 사용
     */
    public Long getUserId() {
        return this.id;
    }
}