package org.forif_backend.common.exception;

import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.domain.product.Product;
import org.forif_backend.domain.study.Study;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    void returnsProductSpecificConflictForProductOptimisticLockFailure() {
        ResponseEntity<ApiResponse<?>> response = exceptionHandler.handleOptimisticLockingFailure(
                new ObjectOptimisticLockingFailureException(Product.class, 1));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().errorCode()).isEqualTo("FOR118-409");
    }

    @Test
    void keepsStudySpecificConflictForStudyOptimisticLockFailure() {
        ResponseEntity<ApiResponse<?>> response = exceptionHandler.handleOptimisticLockingFailure(
                new ObjectOptimisticLockingFailureException(Study.class, 1));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().errorCode()).isEqualTo("FOR138-409");
    }
}
