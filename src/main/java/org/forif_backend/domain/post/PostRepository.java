package org.forif_backend.domain.post;

import java.util.List;
import java.util.Optional;

public interface PostRepository {
    List<Post> findByPostType(String postType);
    List<Post> findByPostType(String postType, String searchKeyword, Long offset, Long limit);
    Optional<Post> findById(Integer id);
    Post save(Post post);
    void deleteById(Integer id);
}
