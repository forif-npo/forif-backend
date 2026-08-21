package org.forif_backend.application.product;

import org.forif_backend.application.file.port.out.FilePort;
import org.forif_backend.application.file.dto.FileInfo;
import org.forif_backend.application.product.dto.CreateProductApplicationCommand;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.product.Product;
import org.forif_backend.domain.product.ProductRepository;
import org.forif_backend.domain.product.ProductSourceType;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    private static final Long APPLICANT_ID = 20260001L;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FilePort filePort;

    @InjectMocks
    private ProductService productService;

    private Product pendingProduct;

    @BeforeEach
    void setUp() {
        User applicant = User.createUser(APPLICANT_ID, "신청자", "applicant@hanyang.ac.kr", "01012345678", "컴퓨터공학부");
        pendingProduct = Product.createPending(
                "before-service", "수정 전 서비스", "한 줄 소개", "상세 소개", ProductSourceType.STUDY,
                null, "Spring", "https://before.example.com", "https://github.com/forif/before", 2026, applicant);
        when(productRepository.findById(anyInt())).thenReturn(Optional.of(pendingProduct));
    }

    @Test
    void removesPreviousThumbnailWhenApplicantRemovesIt() {
        pendingProduct.updateThumbnail("products/thumbnails/old.png");

        productService.updateMyPendingApplication(
                APPLICANT_ID, 1, updateCommand(), true, null);

        verify(filePort).deleteFile("products/thumbnails/old.png");
    }

    @Test
    void updatesTagsWhenApplicantEditsAPendingApplication() {
        productService.updateMyPendingApplication(
                APPLICANT_ID, 1, updateCommand(), false, null);

        assertThat(pendingProduct.getTags()).isEqualTo("Next.js,TypeScript");
    }

    @Test
    void removesThumbnailWhenApplicantDeletesPendingApplication() {
        pendingProduct.updateThumbnail("products/thumbnails/old.png");

        productService.deleteMyPendingApplication(APPLICANT_ID, 1);

        verify(productRepository).delete(pendingProduct);
        verify(filePort).deleteFile("products/thumbnails/old.png");
    }

    @Test
    void removesThumbnailWhenAdminDeletesIt() {
        pendingProduct.updateThumbnail("products/thumbnails/old.png");

        productService.deleteThumbnail(1);

        verify(filePort).deleteFile("products/thumbnails/old.png");
    }

    @Test
    void removesPreviousThumbnailWhenAdminReplacesIt() {
        pendingProduct.updateThumbnail("products/thumbnails/old.png");
        MockMultipartFile replacement = new MockMultipartFile(
                "file", "new.png", "image/png", new byte[]{1, 2, 3});
        when(filePort.uploadFile(any(), anyString())).thenReturn("products/thumbnails/new.png");
        when(filePort.generatePresignedViewUrl("products/thumbnails/new.png"))
                .thenReturn(new FileInfo("products/thumbnails/new.png", "https://files.example.com/new.png"));

        productService.updateThumbnail(1, replacement);

        verify(filePort).deleteFile("products/thumbnails/old.png");
    }

    @Test
    void removesThumbnailWhenAdminDeletesProduct() {
        pendingProduct.updateThumbnail("products/thumbnails/old.png");

        productService.deleteProduct(1);

        verify(productRepository).delete(pendingProduct);
        verify(filePort).deleteFile("products/thumbnails/old.png");
    }

    @Test
    void rejectsModificationForAnotherApplicantsApplication() {
        assertThatThrownBy(() -> productService.updateMyPendingApplication(
                APPLICANT_ID + 1, 1, updateCommand(), false, null))
                .isInstanceOf(ForifException.class)
                .extracting(error -> ((ForifException) error).getErrorCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_PERMISSION);

        verify(productRepository, never()).existsBySlug(anyString());
    }

    @Test
    void rejectsDeletionAfterApplicationHasBeenReviewed() {
        pendingProduct.reject("정보 보완 필요");

        assertThatThrownBy(() -> productService.deleteMyPendingApplication(APPLICANT_ID, 1))
                .isInstanceOf(ForifException.class)
                .extracting(error -> ((ForifException) error).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_NOT_PENDING);

        verify(productRepository, never()).delete(any());
    }

    private CreateProductApplicationCommand updateCommand() {
        return new CreateProductApplicationCommand(
                "수정된 서비스", "updated-service", "수정된 한 줄 소개", "수정된 상세 소개",
                ProductSourceType.SIDE, "https://updated.example.com", "https://github.com/forif/updated",
                List.of("Next.js"), List.of("Next.js", "TypeScript"));
    }
}
