package org.forif_backend.web.post;

import org.forif_backend.application.post.dto.PostDto;
import org.forif_backend.web.post.dto.AnnouncementResponse;
import org.forif_backend.web.post.dto.FAQResponse;
import org.forif_backend.web.post.dto.PostResponse;

import java.time.format.DateTimeFormatter;

public class PostDtoMapper {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public static PostResponse convertToResponse(PostDto dto) {
        return PostResponse.builder()
                .postId(dto.postId())
                .authorId(dto.authorId())
                .authorName(dto.authorName())
                .type(dto.type())
                .title(dto.title())
                .content(dto.content())
                .tag(dto.tag())
                .createdAt(dto.createdAt().format(FORMATTER))
                .build();
    }

    public static AnnouncementResponse toAnnouncementResponse(PostDto dto) {
        return AnnouncementResponse.builder()
                .postId(dto.postId())
                .authorId(dto.authorId())
                .authorName(dto.authorName())
                .type(dto.type())
                .title(dto.title())
                .content(dto.content())
                .tag(dto.tag())
                .createdAt(dto.createdAt().format(FORMATTER))
                .build();
    }

    public static FAQResponse toFAQResponse(PostDto dto) {
        return FAQResponse.builder()
                .postId(dto.postId())
                .authorId(dto.authorId())
                .authorName(dto.authorName())
                .type(dto.type())
                .title(dto.title())
                .content(dto.content())
                .tag(dto.tag())
                .createdAt(dto.createdAt().format(FORMATTER))
                .build();
    }
}
