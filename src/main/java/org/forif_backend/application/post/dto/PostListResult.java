package org.forif_backend.application.post.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record PostListResult(
        List<PostDto> posts
) {
}
