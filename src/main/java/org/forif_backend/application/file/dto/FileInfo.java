package org.forif_backend.application.file.dto;

import lombok.Builder;

@Builder
public record FileInfo(String objectKey, String uploadUrl) {}
