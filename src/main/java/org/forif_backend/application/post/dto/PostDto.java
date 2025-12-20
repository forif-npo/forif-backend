package org.forif_backend.application.post.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record PostDto(
        Integer postId,
        Long authorId,
        String authorName,
        String type,
        String title,
        String content,
        String tag,
        LocalDateTime createdAt
) {
}
