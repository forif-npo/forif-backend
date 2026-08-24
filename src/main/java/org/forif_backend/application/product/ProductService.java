package org.forif_backend.application.product;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.forif_backend.application.product.dto.CreateProductApplicationCommand;
import org.forif_backend.application.file.port.out.FilePort;
import org.forif_backend.application.product.dto.ProductInfo;
import org.forif_backend.application.product.dto.UpdateProductCommand;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.product.Product;
import org.forif_backend.domain.product.ProductMember;
import org.forif_backend.domain.product.ProductRepository;
import org.forif_backend.domain.product.ProductStatus;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import org.forif_backend.application.file.FileViewUrls;
import org.forif_backend.application.file.TransactionalFileCleanup;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private static final String FILE_CLEANUP_CONTEXT = "서비스 썸네일";
    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9](?:[a-z0-9-]{1,18})[a-z0-9]$");
    private static final Set<String> RESERVED_SLUGS = Set.of(
            "www", "dev", "api", "admin", "mail", "apply", "applications", "products", "forif"
    );

    private static final String THUMBNAIL_DIRECTORY = "products/thumbnails";
    private static final long MAX_THUMBNAIL_SIZE = 5 * 1024 * 1024; // 5MB

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final FilePort filePort;

    /** 게시된 서비스 목록 (공개) */
    public List<ProductInfo> getPublishedProducts() {
        return productRepository.findAllPublished().stream()
                .map(this::toInfo)
                .toList();
    }

    /** 게시된 서비스 상세 (공개) */
    public ProductInfo getPublishedProduct(String slug) {
        Product product = productRepository.findBySlug(slug)
                .filter(p -> p.getStatus().isPublished())
                .orElseThrow(() -> new ForifException(ErrorCode.PRODUCT_NOT_FOUND));
        return toInfo(product);
    }

    /** 유저당 동시에 검토 대기 상태로 둘 수 있는 신청 수 */
    private static final int MAX_PENDING_PER_USER = 3;

    /** 서비스 등록 신청 (부원) */
    @Transactional
    public ProductInfo applyProduct(Long userId, CreateProductApplicationCommand command, MultipartFile thumbnail) {
        User applicant = userRepository.findUserById(userId)
                .orElseThrow(() -> new ForifException(ErrorCode.USER_NOT_FOUND));

        String slug = command.slug() == null ? "" : command.slug().trim().toLowerCase();
        validateSlug(slug);
        validatePendingLimit(userId);

        Product product = Product.createPending(
                slug,
                command.name().trim(),
                command.oneLiner().trim(),
                command.description().trim(),
                command.sourceType(),
                joinCsv(command.tags(), 200),
                joinCsv(command.techStack(), 300),
                requireHttpUrl(command.serviceUrl()),
                requireHttpUrl(command.githubUrl()),
                LocalDate.now().getYear(),
                applicant
        );
        product.addMember(ProductMember.create(product, applicant.getUserName(), "신청자"));

        if (thumbnail != null && !thumbnail.isEmpty()) {
            validateImageFile(thumbnail);
            String objectKey = filePort.uploadFile(thumbnail, THUMBNAIL_DIRECTORY);
            product.updateThumbnail(objectKey);
            deleteUploadOnRollback(objectKey);
        }

        try {
            return toInfo(productRepository.save(product));
        } catch (DataIntegrityViolationException e) {
            // slug 중복 검사와 저장 사이의 경합 — UNIQUE 제약이 최종 방어선
            throw new ForifException(ErrorCode.PRODUCT_SLUG_ALREADY_EXISTS);
        }
    }

    /** 내 신청 현황 (부원) */
    public List<ProductInfo> getMyApplications(Long userId) {
        return productRepository.findAllByApplicantId(userId).stream()
                .map(this::toInfo)
                .toList();
    }

    /** 검토 대기 중인 본인 서비스 신청서를 수정한다. */
    @Transactional
    public ProductInfo updateMyPendingApplication(Long userId, Integer productId,
                                                   CreateProductApplicationCommand command,
                                                   boolean removeThumbnail, MultipartFile thumbnail) {
        Product product = getPendingApplicationForApplicant(userId, productId);
        String previousThumbnailObjectKey = product.getThumbnailObjectKey();
        String slug = command.slug() == null ? "" : command.slug().trim().toLowerCase();
        validateSlugForUpdate(product, slug);

        product.updatePendingApplication(
                slug,
                command.name().trim(),
                command.oneLiner().trim(),
                command.description().trim(),
                command.sourceType(),
                joinCsv(command.tags(), 200),
                joinCsv(command.techStack(), 300),
                requireHttpUrl(command.serviceUrl()),
                requireHttpUrl(command.githubUrl())
        );
        try {
            // 다른 신청서가 같은 slug로 먼저 수정한 경우를 여기서 409로 변환한다.
            productRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ForifException(ErrorCode.PRODUCT_SLUG_ALREADY_EXISTS);
        }

        if (removeThumbnail) {
            product.updateThumbnail(null);
        }
        if (thumbnail != null && !thumbnail.isEmpty()) {
            validateImageFile(thumbnail);
            String objectKey = filePort.uploadFile(thumbnail, THUMBNAIL_DIRECTORY);
            product.updateThumbnail(objectKey);
            deleteUploadOnRollback(objectKey);
        }
        if (!Objects.equals(previousThumbnailObjectKey, product.getThumbnailObjectKey())) {
            deleteFileAfterCommit(previousThumbnailObjectKey);
        }
        return toInfo(product);
    }

    /** 검토 대기 중인 본인 서비스 신청서를 삭제한다. */
    @Transactional
    public void deleteMyPendingApplication(Long userId, Integer productId) {
        Product product = getPendingApplicationForApplicant(userId, productId);
        String thumbnailObjectKey = product.getThumbnailObjectKey();
        productRepository.delete(product);
        deleteFileAfterCommit(thumbnailObjectKey);
    }

    // ── 운영진 ──────────────────────────────────────────────────────

    public List<ProductInfo> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::toInfo)
                .toList();
    }

    @Transactional
    public void approveProduct(Integer productId) {
        getProductById(productId).approve();
    }

    @Transactional
    public void rejectProduct(Integer productId, String reason) {
        getProductById(productId).reject(reason);
    }

    @Transactional
    public void changeProductStatus(Integer productId, ProductStatus status) {
        getProductById(productId).changeStatus(status);
    }

    /** 서비스 정보 수정 (운영진) — null 필드는 변경하지 않는다 */
    @Transactional
    public ProductInfo updateProduct(Integer productId, UpdateProductCommand command) {
        Product product = getProductById(productId);
        product.updateInfo(
                blankToNull(command.name()),
                blankToNull(command.oneLiner()),
                blankToNull(command.description()),
                command.sourceLabel() == null ? null : command.sourceLabel().trim(),
                command.tags() == null ? null : joinCsvOrEmpty(command.tags(), 200),
                command.techStack() == null ? null : joinCsvOrEmpty(command.techStack(), 300),
                command.serviceUrl() == null ? null : requireHttpUrlOrEmpty(command.serviceUrl()),
                command.githubUrl() == null ? null : requireHttpUrlOrEmpty(command.githubUrl())
        );
        return toInfo(product);
    }

    /** 썸네일 이미지 등록·교체 (운영진) */
    @Transactional
    public String updateThumbnail(Integer productId, MultipartFile file) {
        Product product = getProductById(productId);
        String previousThumbnailObjectKey = product.getThumbnailObjectKey();
        validateImageFile(file);

        String objectKey = filePort.uploadFile(file, THUMBNAIL_DIRECTORY);
        product.updateThumbnail(objectKey);
        deleteUploadOnRollback(objectKey);
        if (!Objects.equals(previousThumbnailObjectKey, objectKey)) {
            deleteFileAfterCommit(previousThumbnailObjectKey);
        }

        return toFileViewUrl(objectKey);
    }

    /** 썸네일 제거 (운영진) */
    @Transactional
    public void deleteThumbnail(Integer productId) {
        Product product = getProductById(productId);
        String thumbnailObjectKey = product.getThumbnailObjectKey();
        product.updateThumbnail(null);
        deleteFileAfterCommit(thumbnailObjectKey);
    }

    @Transactional
    public void deleteProduct(Integer productId) {
        Product product = getProductById(productId);
        String thumbnailObjectKey = product.getThumbnailObjectKey();
        productRepository.delete(product);
        deleteFileAfterCommit(thumbnailObjectKey);
    }

    // ── 내부 유틸 ────────────────────────────────────────────────────

    private Product getProductById(Integer productId) {
        return productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ForifException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    private Product getPendingApplicationForApplicant(Long userId, Integer productId) {
        Product product = getProductById(productId);
        if (!product.getApplicant().getId().equals(userId)) {
            throw new ForifException(ErrorCode.INSUFFICIENT_PERMISSION);
        }
        if (product.getStatus() != ProductStatus.PENDING) {
            throw new ForifException(ErrorCode.PRODUCT_NOT_PENDING);
        }
        return product;
    }

    private void validateSlug(String slug) {
        if (!SLUG_PATTERN.matcher(slug).matches() || RESERVED_SLUGS.contains(slug)) {
            throw new ForifException(ErrorCode.PRODUCT_SLUG_INVALID);
        }
        if (productRepository.existsBySlug(slug)) {
            throw new ForifException(ErrorCode.PRODUCT_SLUG_ALREADY_EXISTS);
        }
    }

    private void validateSlugForUpdate(Product product, String slug) {
        if (!SLUG_PATTERN.matcher(slug).matches() || RESERVED_SLUGS.contains(slug)) {
            throw new ForifException(ErrorCode.PRODUCT_SLUG_INVALID);
        }
        if (!slug.equals(product.getSlug()) && productRepository.existsBySlug(slug)) {
            throw new ForifException(ErrorCode.PRODUCT_SLUG_ALREADY_EXISTS);
        }
    }

    private void validatePendingLimit(Long userId) {
        long pendingCount = productRepository.countByApplicantIdAndStatus(userId, ProductStatus.PENDING);
        if (pendingCount >= MAX_PENDING_PER_USER) {
            throw new ForifException(ErrorCode.PRODUCT_PENDING_LIMIT);
        }
    }

    private String joinCsv(List<String> values, int maxLength) {
        if (values == null) return null;
        List<String> cleaned = values.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        if (cleaned.isEmpty()) return null;

        String joined = String.join(",", cleaned);
        if (joined.length() > maxLength) {
            throw new ForifException(ErrorCode.PRODUCT_INPUT_TOO_LONG);
        }
        return joined;
    }

    private ProductInfo toInfo(Product product) {
        return ProductInfo.from(product, toFileViewUrl(product.getThumbnailObjectKey()));
    }

    private String toFileViewUrl(String objectKey) {
        return FileViewUrls.resolveViewUrl(filePort, objectKey);
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_THUMBNAIL_SIZE) {
            throw new ForifException(ErrorCode.INVALID_FILE_ATTACHMENT);
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ForifException(ErrorCode.INVALID_FILE_ATTACHMENT);
        }
    }

    /** 수정 요청에서 빈 리스트는 "값 비우기"를 뜻하므로 빈 문자열로 남긴다 */
    private String joinCsvOrEmpty(List<String> values, int maxLength) {
        String joined = joinCsv(values, maxLength);
        return joined == null ? "" : joined;
    }

    private String requireHttpUrlOrEmpty(String value) {
        if (value.isBlank()) {
            return "";
        }
        return requireHttpUrl(value);
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    /** 링크로 렌더링되는 값이므로 http(s) 스킴만 허용한다 (javascript: 등 주입 방지) */
    private String requireHttpUrl(String value) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        String lower = trimmed.toLowerCase();
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            throw new ForifException(ErrorCode.PRODUCT_URL_INVALID);
        }
        return trimmed;
    }

    /** 슬러그 경합 등으로 신청이 롤백되면 방금 올린 썸네일이 고아로 남지 않게 회수한다 */
    private void deleteUploadOnRollback(String objectKey) {
        TransactionalFileCleanup.deleteOnRollback(filePort, objectKey, FILE_CLEANUP_CONTEXT);
    }

    /** DB 반영이 끝난 뒤에만 더 이상 참조되지 않는 기존 파일을 삭제한다. */
    private void deleteFileAfterCommit(String objectKey) {
        TransactionalFileCleanup.deleteAfterCommit(filePort, objectKey, FILE_CLEANUP_CONTEXT);
    }
}
