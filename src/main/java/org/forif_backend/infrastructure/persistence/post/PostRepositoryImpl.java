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
    public List<Post> searchWithCursor(String postType, String searchKeyword, Integer cursor, int size) {
        return postQueryRepository.searchWithCursor(postType, searchKeyword, cursor, size);
    }

    @Override
    public List<Post> searchWithOffset(String postType, String searchKeyword, int page, int size) {
        return postQueryRepository.searchWithOffset(postType, searchKeyword, page, size);
    }

    @Override
    public long countByPostType(String postType, String searchKeyword) {
        return postQueryRepository.countByPostType(postType, searchKeyword);
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
