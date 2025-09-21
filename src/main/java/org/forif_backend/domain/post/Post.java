package org.forif_backend.domain.post;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.forif_backend.common.BaseTimeEntity;
import org.forif_backend.domain.user.User;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tb_post")
public class Post extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(length = 50, nullable = false)
    private String postType;

    @Column(length = 100)
    private String title;

    @Column(length = 5000)
    private String content;

    @Column(length = 100)
    private String tag;

    // 생성자나 정적 팩토리 메서드로만 생성
    private Post(User user, String postType, String title, String content, String tag) {
        this.user = user;
        this.postType = postType;
        this.title = title;
        this.content = content;
        this.tag = tag;
    }

    public static Post createPost(User user, String postType, String title, String content, String tag) {
        return new Post(user, postType, title, content, tag);
    }
}