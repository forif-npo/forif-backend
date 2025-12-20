package org.forif_backend.web.post.dto;

import lombok.Builder;

@Builder
public record AnnouncementResponse(
        Integer postId,
        Long authorId,
        String authorName,
        String type,
        String title,
        String content,
        String tag,
        String createdAt
) {
}
