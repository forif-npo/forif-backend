package org.forif_backend.web.post.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record FAQResponse(
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
