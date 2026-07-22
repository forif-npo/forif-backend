package org.forif_backend.infrastructure.persistence.product;

import org.forif_backend.domain.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductJpaRepository extends JpaRepository<Product, Integer> {

    Optional<Product> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query("""
            SELECT p FROM Product p
            WHERE p.status IN (org.forif_backend.domain.product.ProductStatus.LIVE,
                               org.forif_backend.domain.product.ProductStatus.DEV,
                               org.forif_backend.domain.product.ProductStatus.PAUSED,
                               org.forif_backend.domain.product.ProductStatus.RETIRED)
            ORDER BY p.actYear DESC, p.id DESC
            """)
    List<Product> findAllPublished();

    @Query("SELECT p FROM Product p WHERE p.applicant.id = :userId ORDER BY p.id DESC")
    List<Product> findAllByApplicantId(@Param("userId") Long userId);

    List<Product> findAllByOrderByIdDesc();
}
