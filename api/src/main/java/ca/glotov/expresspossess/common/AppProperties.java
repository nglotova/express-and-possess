package ca.glotov.expresspossess.common;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Settings under the {@code app} prefix in application.yml.
 *
 * @param baseUrl          where the web app is served; used to build links in emails
 * @param mailFrom         sender address for every email
 * @param passwordResetTtl how long a password reset link stays valid
 * @param invitationTtl    how long a group invitation link stays valid
 * @param uploadsDir       directory for uploaded pictures
 * @param notificationsTopic Kafka topic the outbox is relayed to
 * @param outboxPollInterval how often the outbox publisher looks for unpublished events
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(String baseUrl, String mailFrom, Duration passwordResetTtl, Duration invitationTtl,
                            String uploadsDir, String notificationsTopic, Duration outboxPollInterval) {
}
