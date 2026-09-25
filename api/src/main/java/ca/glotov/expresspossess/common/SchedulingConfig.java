package ca.glotov.expresspossess.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on {@code @Scheduled}: the outbox publisher, the demo reset and the retries of link pictures. */
@Configuration
@EnableScheduling
class SchedulingConfig {
}
