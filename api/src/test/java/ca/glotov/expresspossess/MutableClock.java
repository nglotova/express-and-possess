package ca.glotov.expresspossess;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * A clock tests can push forward, to expire tokens without waiting.
 */
public class MutableClock extends Clock {

    private final Clock base;
    private Duration offset = Duration.ZERO;

    public MutableClock(Clock base) {
        this.base = base;
    }

    public void advance(Duration by) {
        offset = offset.plus(by);
    }

    public void reset() {
        offset = Duration.ZERO;
    }

    @Override
    public ZoneId getZone() {
        return base.getZone();
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return base.instant().plus(offset);
    }
}
