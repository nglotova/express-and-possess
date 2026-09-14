package ca.glotov.expresspossess;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * One PostgreSQL container shared by every test that imports this configuration.
 * Spring Boot wires the datasource from the container through {@link ServiceConnection}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestSupport {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>("postgres:17-alpine");
    }
}
