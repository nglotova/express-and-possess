package ca.glotov.expresspossess.expressions;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings under {@code app.link-preview}.
 *
 * @param enabled               take a picture from the first link when the creator adds none
 * @param allowPrivateAddresses fetch from local and private network addresses; only for tests,
 *                              which serve the shop pages from the test machine itself
 */
@ConfigurationProperties(prefix = "app.link-preview")
public record LinkPreviewProperties(boolean enabled, boolean allowPrivateAddresses) {
}
