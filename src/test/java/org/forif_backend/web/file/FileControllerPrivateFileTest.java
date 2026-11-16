package org.forif_backend.web.file;

import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileControllerPrivateFileTest {

    @TempDir Path root;

    @Test
    void 지원서_첨부파일은_공개_파일_경로에서_받을_수_없다() throws Exception {
        Path privateFile = root.resolve("staff-applications/12/portfolio.pdf");
        Files.createDirectories(privateFile.getParent());
        Files.writeString(privateFile, "private");
        FileController controller = new FileController();
        ReflectionTestUtils.setField(controller, "rootPath", root.toString());
        MockHttpServletRequest request = new MockHttpServletRequest("GET",
                "/api/v1/files/staff-applications/12/portfolio.pdf");

        assertThatThrownBy(() -> controller.getFile(request, false))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FILE_NOT_FOUND);
    }
}
