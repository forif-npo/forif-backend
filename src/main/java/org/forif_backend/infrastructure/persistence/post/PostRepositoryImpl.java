package org.forif_backend.infrastructure.persistence.post;

import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.post.Post;
import org.forif_backend.domain.post.PostRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PostRepositoryImpl implements PostRepository {

    private final PostJpaRepository postJpaRepository;
    private final PostQueryRepository postQueryRepository;

    @Override
    public List<Post> findByPostType(String postType) {
        return postJpaRepository.findByPostType(postType);
    }

    @Override
    public List<Post> findByPostType(String postType, String searchKeyword, Long offset, Long limit) {
        return postQueryRepository.findByPostTypeWithPagination(postType, searchKeyword, offset, limit);
    }

    @Override
    public Optional<Post> findById(Integer id) {
        return postJpaRepository.findById(id);
    }

    @Override
    public Post save(Post post) {
        return postJpaRepository.save(post);
    }

    @Override
    public void deleteById(Integer id) {
        postJpaRepository.deleteById(id);
    }
}
