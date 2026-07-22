package org.forif_backend.web.product;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.RequiredArgsConstructor;
import org.forif_backend.application.product.ProductService;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.domain.product.ProductStatus;
import org.forif_backend.web.product.dto.AdminProductResponse;
import org.hibernate.validator.constraints.Length;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "프로덕트 관리 (운영진)", description = "프로덕트 등록 신청 검토 및 게시 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/products")
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductController {

    private final ProductService productService;

    @Operation(summary = "프로덕트 전체 목록 (운영진)", description = "검토 대기 신청을 포함한 모든 프로덕트를 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminProductResponse>>> getAllProducts() {
        return ResponseEntity.ok(
                ApiResponse.success(AdminProductResponse.fromList(productService.getAllProducts())));
    }

    @Operation(summary = "등록 신청 승인", description = "검토 대기 신청을 승인해 서비스 중 상태로 게시합니다.")
    @PatchMapping("/{productId}/approve")
    public ResponseEntity<ApiResponse<Void>> approveProduct(
            @Parameter(description = "프로덕트 ID") @PathVariable Integer productId
    ) {
        productService.approveProduct(productId);
        return ResponseEntity.ok(ApiResponse.successWithMsg("Success"));
    }

    @Operation(summary = "등록 신청 반려", description = "검토 대기 신청을 사유와 함께 반려합니다.")
    @PatchMapping("/{productId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectProduct(
            @Parameter(description = "프로덕트 ID") @PathVariable Integer productId,
            @Valid @RequestBody RejectProductRequest request
    ) {
        productService.rejectProduct(productId, request.getRejectReason());
        return ResponseEntity.ok(ApiResponse.successWithMsg("Success"));
    }

    @Operation(summary = "게시 상태 변경", description = "게시된 프로덕트의 상태(LIVE/DEV/PAUSED/RETIRED)를 변경합니다.")
    @PatchMapping("/{productId}/status")
    public ResponseEntity<ApiResponse<Void>> changeProductStatus(
            @Parameter(description = "프로덕트 ID") @PathVariable Integer productId,
            @Valid @RequestBody UpdateProductStatusRequest request
    ) {
        productService.changeProductStatus(productId, request.getStatus());
        return ResponseEntity.ok(ApiResponse.successWithMsg("Success"));
    }

    @Operation(summary = "프로덕트 삭제", description = "프로덕트(신청 포함)를 삭제합니다.")
    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @Parameter(description = "프로덕트 ID") @PathVariable Integer productId
    ) {
        productService.deleteProduct(productId);
        return ResponseEntity.ok(ApiResponse.successWithMsg("Success"));
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class RejectProductRequest {
        @NotBlank
        @Length(max = 500)
        private String rejectReason;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class UpdateProductStatusRequest {
        @NotNull
        private ProductStatus status;
    }
}
