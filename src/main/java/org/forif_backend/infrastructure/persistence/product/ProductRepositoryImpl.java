package org.forif_backend.infrastructure.persistence.product;

import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.product.Product;
import org.forif_backend.domain.product.ProductRepository;
import org.forif_backend.domain.product.ProductStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductJpaRepository productJpaRepository;

    @Override
    public Product save(Product product) {
        return productJpaRepository.save(product);
    }

    @Override
    public void flush() {
        productJpaRepository.flush();
    }

    @Override
    public Optional<Product> findById(Integer productId) {
        return productJpaRepository.findById(productId);
    }

    @Override
    public Optional<Product> findByIdForUpdate(Integer productId) {
        return productJpaRepository.findByIdForUpdate(productId);
    }

    @Override
    public Optional<Product> findBySlug(String slug) {
        return productJpaRepository.findBySlug(slug);
    }

    @Override
    public boolean existsBySlug(String slug) {
        return productJpaRepository.existsBySlug(slug);
    }

    @Override
    public long countByApplicantIdAndStatus(Long userId, ProductStatus status) {
        return productJpaRepository.countByApplicantIdAndStatus(userId, status);
    }

    @Override
    public List<Product> findAllLive() {
        return productJpaRepository.findAllLive();
    }

    @Override
    public List<Product> findAllByApplicantId(Long userId) {
        return productJpaRepository.findAllByApplicantId(userId);
    }

    @Override
    public List<Product> findAll() {
        return productJpaRepository.findAllForAdmin();
    }

    @Override
    public void delete(Product product) {
        productJpaRepository.delete(product);
    }
}
