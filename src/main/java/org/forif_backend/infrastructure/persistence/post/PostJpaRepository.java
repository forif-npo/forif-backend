package org.forif_backend.infrastructure.persistence.post;

import org.forif_backend.domain.post.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostJpaRepository extends JpaRepository<Post, Integer> {
    List<Post> findByPostType(String postType);
}
