package ca.glotov.expresspossess.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on {@code @Scheduled}; the outbox publisher is the one scheduled job. */
@Configuration
@EnableScheduling
class SchedulingConfig {
}
