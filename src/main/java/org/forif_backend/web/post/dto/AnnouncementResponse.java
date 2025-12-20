package org.forif_backend.web.post.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record AnnouncementResponse(
        Integer postId,
        Long authorId,
        String authorName,
        String type,
        String title,
        String content,
        String tag,
        String createdAt,
        List<String> imageUrls
) {
}
