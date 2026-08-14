package org.forif_backend.web.product;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.forif_backend.application.product.ProductService;
import org.forif_backend.application.product.dto.ProductInfo;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.web.product.dto.CreateProductApplicationRequest;
import org.forif_backend.web.product.dto.ProductApplicationResponse;
import org.forif_backend.web.product.dto.ProductDetailResponse;
import org.forif_backend.web.product.dto.ProductResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "서비스", description = "부원 서비스 쇼케이스 및 등록 신청 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "서비스 목록 조회", description = "게시된(승인된) 서비스 목록을 조회합니다. 인증 없이 접근 가능합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getProducts() {
        List<ProductInfo> products = productService.getPublishedProducts();
        return ResponseEntity.ok(ApiResponse.success(ProductResponse.fromList(products)));
    }

    @Operation(summary = "내 서비스 등록 신청 현황", description = "로그인한 부원의 서비스 등록 신청 목록과 검토 결과를 조회합니다.")
    @GetMapping("/applications/me")
    public ResponseEntity<ApiResponse<List<ProductApplicationResponse>>> getMyApplications(
            @AuthenticationPrincipal Long userId
    ) {
        List<ProductInfo> applications = productService.getMyApplications(userId);
        return ResponseEntity.ok(ApiResponse.success(ProductApplicationResponse.fromList(applications)));
    }

    @Operation(summary = "서비스 등록 신청", description = "부원이 직접 만든 서비스의 등록을 신청합니다. request(JSON)와 선택 썸네일 이미지 파일을 multipart/form-data로 전송합니다. 운영진 승인 후 목록에 게시됩니다.")
    @PostMapping(value = "/applications", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProductApplicationResponse>> applyProduct(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestPart("request") CreateProductApplicationRequest request,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail
    ) {
        ProductInfo info = productService.applyProduct(userId, request.toCommand(), thumbnail);
        return ResponseEntity.ok(ApiResponse.success(ProductApplicationResponse.from(info)));
    }

    /** 썸네일 없이 JSON으로 보내던 기존 클라이언트가 415를 받지 않도록 병행 유지한다 */
    @Operation(summary = "서비스 등록 신청 (JSON)", description = "썸네일 없이 JSON 본문만으로 서비스 등록을 신청합니다.")
    @PostMapping(value = "/applications", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ProductApplicationResponse>> applyProductJson(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateProductApplicationRequest request
    ) {
        ProductInfo info = productService.applyProduct(userId, request.toCommand(), null);
        return ResponseEntity.ok(ApiResponse.success(ProductApplicationResponse.from(info)));
    }

    @Operation(summary = "서비스 상세 조회", description = "게시된 서비스의 상세 정보를 조회합니다. 인증 없이 접근 가능합니다.")
    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProduct(
            @Parameter(description = "서비스 슬러그(서브도메인)") @PathVariable String slug
    ) {
        ProductInfo product = productService.getPublishedProduct(slug);
        return ResponseEntity.ok(ApiResponse.success(ProductDetailResponse.from(product)));
    }
}
