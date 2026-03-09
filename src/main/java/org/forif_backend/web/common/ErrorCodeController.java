package org.forif_backend.web.common;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.common.exception.ErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@Tag(name = "에러코드 참조", description = "API에서 반환될 수 있는 모든 에러코드 목록")
@RestController
@RequestMapping("/api/v1/error-codes")
public class ErrorCodeController {

    record ErrorCodeInfo(String code, int httpStatus, String message) {}

    @Operation(
        summary = "전체 에러코드 목록 조회",
        description = """
            API 전반에서 반환될 수 있는 모든 에러코드 목록입니다.

            **번호 체계**
            - `FOR001` ~ `FOR041` : 4xx 클라이언트 오류
            - `FOR100` ~ `FOR102` : 5xx 서버 오류

            **접미사 의미**
            - `-400` Bad Request: 잘못된 요청
            - `-401` Unauthorized: 인증 실패 (토큰 없음/만료/무효)
            - `-403` Forbidden: 권한 없음 (인증은 됐으나 접근 불가)
            - `-404` Not Found: 리소스 없음
            - `-409` Conflict: 중복/충돌
            - `-500` Internal Server Error: 서버 오류
            """
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<ErrorCodeInfo>>> getErrorCodes() {
        List<ErrorCodeInfo> errorCodes = Arrays.stream(ErrorCode.values())
            .map(e -> new ErrorCodeInfo(e.getCode(), e.getHttpStatus().value(), e.getMessage()))
            .toList();
        return ResponseEntity.ok(ApiResponse.success(errorCodes));
    }
}
