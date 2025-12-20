package org.forif_backend.web.post.dto;

import lombok.Builder;

@Builder
public record AnnouncementRequest(
        String title,
        String content,
        String tag
) {
}
