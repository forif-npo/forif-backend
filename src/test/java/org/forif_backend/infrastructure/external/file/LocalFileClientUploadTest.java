package org.forif_backend.infrastructure.external.file;

import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LocalFileClientUploadTest {

    @TempDir Path root;

    @Test
    void 쓰기_실패_후_부분_파일을_남기지_않는다() throws Exception {
        LocalFileClient client = new LocalFileClient(root.toString(), "");
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("portfolio.pdf");
        doAnswer(invocation -> {
            Path target = invocation.getArgument(0);
            Files.writeString(target, "partial");
            throw new IOException("write failed");
        }).when(file).transferTo(any(Path.class));

        assertThatThrownBy(() -> client.uploadFile(file, "staff-applications/12"))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INTERNAL_SERVER_ERROR);
        try (var paths = Files.walk(root)) {
            assertThat(paths.filter(Files::isRegularFile).count()).isZero();
        }
    }
}
