package org.forif_backend.application.product;

import lombok.RequiredArgsConstructor;
import org.forif_backend.application.product.dto.CreateProductApplicationCommand;
import org.forif_backend.application.product.dto.ProductInfo;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9](?:[a-z0-9-]{1,18})[a-z0-9]$");
    private static final Set<String> RESERVED_SLUGS = Set.of(
            "www", "dev", "api", "admin", "mail", "apply", "applications", "products", "forif"
    );

    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    /** 게시된 프로덕트 목록 (공개) */
    public List<ProductInfo> getPublishedProducts() {
        return productRepository.findAllPublished().stream()
                .map(ProductInfo::from)
                .toList();
    }

    /** 게시된 프로덕트 상세 (공개) */
    public ProductInfo getPublishedProduct(String slug) {
        Product product = productRepository.findBySlug(slug)
                .filter(p -> p.getStatus().isPublished())
                .orElseThrow(() -> new ForifException(ErrorCode.PRODUCT_NOT_FOUND));
        return ProductInfo.from(product);
    }

    /** 유저당 동시에 검토 대기 상태로 둘 수 있는 신청 수 */
    private static final int MAX_PENDING_PER_USER = 3;

    /** 프로덕트 등록 신청 (부원) */
    @Transactional
    public ProductInfo applyProduct(Long userId, CreateProductApplicationCommand command) {
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

        try {
            return ProductInfo.from(productRepository.save(product));
        } catch (DataIntegrityViolationException e) {
            // slug 중복 검사와 저장 사이의 경합 — UNIQUE 제약이 최종 방어선
            throw new ForifException(ErrorCode.PRODUCT_SLUG_ALREADY_EXISTS);
        }
    }

    /** 내 신청 현황 (부원) */
    public List<ProductInfo> getMyApplications(Long userId) {
        return productRepository.findAllByApplicantId(userId).stream()
                .map(ProductInfo::from)
                .toList();
    }

    // ── 운영진 ──────────────────────────────────────────────────────

    public List<ProductInfo> getAllProducts() {
        return productRepository.findAll().stream()
                .map(ProductInfo::from)
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

    @Transactional
    public void deleteProduct(Integer productId) {
        productRepository.delete(getProductById(productId));
    }

    // ── 내부 유틸 ────────────────────────────────────────────────────

    private Product getProductById(Integer productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ForifException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    private void validateSlug(String slug) {
        if (!SLUG_PATTERN.matcher(slug).matches() || RESERVED_SLUGS.contains(slug)) {
            throw new ForifException(ErrorCode.PRODUCT_SLUG_INVALID);
        }
        if (productRepository.existsBySlug(slug)) {
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
}
