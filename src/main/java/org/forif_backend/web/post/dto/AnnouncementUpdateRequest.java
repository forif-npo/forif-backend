package org.forif_backend.web.post.dto;

import lombok.Builder;

@Builder
public record AnnouncementUpdateRequest(
        String title,
        String content,
        String tag
) {
}
