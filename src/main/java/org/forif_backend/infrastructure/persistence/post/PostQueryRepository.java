package org.forif_backend.infrastructure.persistence.post;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.forif_backend.domain.post.Post;
import org.forif_backend.domain.post.QPost;
import org.forif_backend.domain.user.QUser;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PostQueryRepository {
    private final JPAQueryFactory queryFactory;
    private final QPost post = QPost.post;
    private final QUser user = QUser.user;

    public PostQueryRepository(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    public List<Post> findByPostTypeWithPagination(String postType, String searchKeyword, Long offset, Long limit) {
        return queryFactory
                .selectFrom(post)
                .leftJoin(post.user, user).fetchJoin()
                .where(
                        post.postType.eq(postType),
                        titleContains(searchKeyword)
                )
                .orderBy(post.createdAt.desc())
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    private BooleanExpression titleContains(String searchKeyword) {
        if (searchKeyword == null || searchKeyword.trim().isEmpty()) {
            return null;
        }
        return post.title.containsIgnoreCase(searchKeyword);
    }
}
