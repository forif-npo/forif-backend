package org.forif_backend.infrastructure.persistence.post;

import org.forif_backend.domain.post.PostFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostFileJpaRepository extends JpaRepository<PostFile, Long> {
    List<PostFile> findByPostId(Integer postId);
    List<PostFile> findByPost_IdOrderByFileNumAsc(Integer postId);
    void deleteByPostId(Integer postId);
}
