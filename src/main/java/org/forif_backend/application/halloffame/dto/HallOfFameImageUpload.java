package org.forif_backend.application.halloffame.dto;

/**
 * 전시 이미지 업로드 결과.
 * 업로드도 항목을 수정하므로 version이 올라간다. 화면이 상세를 다시 불러오지 않아도
 * 다음 수정·게시 요청을 보낼 수 있도록 올라간 version을 함께 돌려준다.
 */
public record HallOfFameImageUpload(
        String imageUrl,
        Long version
) {
}
