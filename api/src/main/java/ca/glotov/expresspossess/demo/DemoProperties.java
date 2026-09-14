package ca.glotov.expresspossess.demo;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings for the public demo instance. Off by default; the family instance never
 * enables it.
 *
 * @param enabled   seed a fictional family on startup and offer "log in as" buttons
 * @param resetCron when to wipe and reseed (Spring cron, six fields)
 * @param password  the password every demo persona gets
 */
@ConfigurationProperties(prefix = "app.demo")
public record DemoProperties(boolean enabled, String resetCron, String password) {
}
