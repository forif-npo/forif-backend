package org.forif_backend.domain.post;

import java.util.List;
import java.util.Optional;

public interface PostRepository {
    List<Post> findByPostType(String postType);
    List<Post> searchWithCursor(String postType, String searchKeyword, Integer cursor, int size);
    List<Post> searchWithOffset(String postType, String searchKeyword, int page, int size);
    long countByPostType(String postType, String searchKeyword);
    Optional<Post> findById(Integer id);
    Post save(Post post);
    void deleteById(Integer id);
}
