package ca.glotov.expresspossess.settings;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** The one row of site-wide settings. */
@Entity
@Table(name = "site_settings")
public class SiteSettings {

    static final short ID = 1;

    @Id
    private Short id;

    @Column(name = "invitations_per_day", nullable = false)
    private int invitationsPerDay;

    protected SiteSettings() {
    }

    public int getInvitationsPerDay() {
        return invitationsPerDay;
    }

    void setInvitationsPerDay(int invitationsPerDay) {
        this.invitationsPerDay = invitationsPerDay;
    }
}
