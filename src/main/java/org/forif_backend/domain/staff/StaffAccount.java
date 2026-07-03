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
@Table(name = "tb_staff_account", uniqueConstraints = {
        // 한 유저가 역할(MENTOR/ADMIN)별로 계정을 하나씩 가질 수 있다
        @UniqueConstraint(columnNames = {"user_id", "role"})
})
public class StaffAccount extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "staff_account_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 100, nullable = false)
    private String password;

    @Column(length = 50, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private StaffRole role;

    @Column(length = 100, nullable = false)
    private String affiliation; // 멘토일 경우 스터디명, 어드민일 경우 팀명 (예: 기획팀, 인공지능 스터디)

    private StaffAccount(User user, String password, String name, StaffRole role, String affiliation) {
        this.user = user;
        this.password = password;
        this.name = name;
        this.role = role;
        this.affiliation = affiliation;
    }

    public static StaffAccount createStaffAccount(User user, String password, String name, StaffRole role, String affiliation) {
        return new StaffAccount(user, password, name, role, affiliation);
    }

    /**
     * User ID 반환
     * StaffAccountService에서 사용
     */
    public Long getUserId() {
        return this.user.getId();
    }

    public void updateName(String name) {
        this.name = name;
    }

    public void updatePassword(String password) {
        this.password = password;
    }

    public void updateAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }

    /**
     * 멘토 정보 수정
     */
    public void updateInfo(String name, String password, String affiliation) {
        if (name != null) this.name = name;
        if (password != null) this.password = password;
        if (affiliation != null) this.affiliation = affiliation;
    }
}