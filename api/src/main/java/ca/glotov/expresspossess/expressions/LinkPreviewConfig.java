package ca.glotov.expresspossess.expressions;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Pictures from links: fetched on a background thread after a change commits, and kept in a
 * cache so a link previewed while typing is not fetched again.
 */
@Configuration
@EnableAsync
@EnableCaching
@EnableConfigurationProperties(LinkPreviewProperties.class)
class LinkPreviewConfig {
}
