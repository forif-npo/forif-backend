package org.forif_backend.infrastructure.persistence.post;

import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.post.PostFile;
import org.forif_backend.domain.post.PostFileRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class PostFileRepositoryImpl implements PostFileRepository {

    private final PostFileJpaRepository postFileJpaRepository;

    @Override
    public PostFile save(PostFile postFile) {
        return postFileJpaRepository.save(postFile);
    }

    @Override
    public List<PostFile> findByPostId(Integer postId) {
        return postFileJpaRepository.findByPostId(postId);
    }

    @Override
    public void deleteByPostId(Integer postId) {
        postFileJpaRepository.deleteByPostId(postId);
    }
}
