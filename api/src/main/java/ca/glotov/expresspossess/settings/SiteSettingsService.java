package ca.glotov.expresspossess.settings;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Settings a site administrator changes on the administration page. The row is created by
 * the migration, so it always exists.
 */
@Service
@Transactional
public class SiteSettingsService {

    private final SiteSettingsRepository settings;

    SiteSettingsService(SiteSettingsRepository settings) {
        this.settings = settings;
    }

    /** How many invitation emails one member may send in 24 hours. Site administrators have no limit. */
    @Transactional(readOnly = true)
    public int invitationsPerDay() {
        return row().getInvitationsPerDay();
    }

    public void setInvitationsPerDay(int invitationsPerDay) {
        row().setInvitationsPerDay(invitationsPerDay);
    }

    private SiteSettings row() {
        return settings.findById(SiteSettings.ID).orElseThrow();
    }
}
