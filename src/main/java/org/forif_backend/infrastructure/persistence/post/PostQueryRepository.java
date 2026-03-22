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

    public List<Post> searchWithCursor(String postType, String searchKeyword, Integer cursor, int size) {
        return queryFactory
                .selectFrom(post)
                .leftJoin(post.user, user).fetchJoin()
                .where(
                        post.postType.eq(postType),
                        cursorLt(cursor),
                        titleContains(searchKeyword)
                )
                .orderBy(post.id.desc())
                .limit(size + 1)
                .fetch();
    }

    public long countByPostType(String postType, String searchKeyword) {
        Long count = queryFactory
                .select(post.count())
                .from(post)
                .where(
                        post.postType.eq(postType),
                        titleContains(searchKeyword)
                )
                .fetchOne();
        return count != null ? count : 0L;
    }

    private BooleanExpression cursorLt(Integer cursor) {
        if (cursor == null) {
            return null;
        }
        return post.id.lt(cursor);
    }

    private BooleanExpression titleContains(String searchKeyword) {
        if (searchKeyword == null || searchKeyword.trim().isEmpty()) {
            return null;
        }
        return post.title.containsIgnoreCase(searchKeyword);
    }
}
