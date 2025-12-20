package org.forif_backend.domain.post;

import java.util.List;

public interface PostFileRepository {
    PostFile save(PostFile postFile);
    List<PostFile> findByPostId(Integer postId);
    List<PostFile> findByPostIdOrderByFileNum(Integer postId);
    void deleteByPostId(Integer postId);
}
