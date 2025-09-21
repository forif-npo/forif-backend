package org.forif_backend.domain.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.forif_backend.common.BaseTimeEntity;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tb_user")
public class User extends BaseTimeEntity {

    @Id
    @Column(name = "user_id")
    private Long id;

    @Column(length = 50)
    private String userName;

    @Column(length = 100, nullable = false, unique = true)
    private String email;

    @Column(length = 20)
    private String phoneNum;

    @Column(length = 50)
    private String department;

    @Column(length = 300)
    private String imgUrl;

    // 생성자나 정적 팩토리 메서드로만 생성
    private User(Long id, String userName, String email) {
        this.id = id;
        this.userName = userName;
        this.email = email;
    }

    public static User createUser(Long id, String userName, String email) {
        return new User(id, userName, email);
    }
}