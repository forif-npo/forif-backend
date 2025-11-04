package org.forif_backend.common.dto.request;

import lombok.Setter;
import lombok.Getter;

@Getter
@Setter
public class PageRequest {
    private Long page = 0L;      // 기본값
    private Long pageSize = 20L; // 기본값
    private SortOrder sortOrder;
    private String sortBy;
}