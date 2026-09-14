package ca.glotov.expresspossess;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.Clock;

/**
 * Real infrastructure for integration tests: PostgreSQL for the schema, Mailpit to catch
 * the emails the application sends. Spring caches the context, so every test class that
 * imports this shares one set of containers.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>("postgres:17-alpine");
    }

    @Bean
    GenericContainer<?> mailpit() {
        return new GenericContainer<>("axllent/mailpit:v1.24").withExposedPorts(1025, 8025);
    }

    @Bean
    DynamicPropertyRegistrar testProperties(GenericContainer<?> mailpit) {
        return registry -> {
            registry.add("spring.mail.host", mailpit::getHost);
            registry.add("spring.mail.port", () -> mailpit.getMappedPort(1025));
            registry.add("app.uploads-dir", () -> "target/test-uploads");
        };
    }

    @Bean
    Mailpit mailpitClient(GenericContainer<?> mailpit) {
        return new Mailpit("http://" + mailpit.getHost() + ":" + mailpit.getMappedPort(8025));
    }

    @Bean
    @Primary
    MutableClock testClock() {
        return new MutableClock(Clock.systemUTC());
    }
}
