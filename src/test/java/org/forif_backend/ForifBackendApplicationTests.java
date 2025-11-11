package org.forif_backend;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@Disabled("Redis configuration required - temporarily disabled for CI/CD")
@SpringBootTest
@ActiveProfiles("test")
class ForifBackendApplicationTests {

    @Test
    void contextLoads() {
    }

}
