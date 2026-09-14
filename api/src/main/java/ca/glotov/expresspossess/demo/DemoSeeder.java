package ca.glotov.expresspossess.demo;

import ca.glotov.expresspossess.auth.AccountService;
import ca.glotov.expresspossess.auth.Role;
import ca.glotov.expresspossess.auth.User;
import ca.glotov.expresspossess.auth.UserRepository;
import ca.glotov.expresspossess.expressions.ExpressionService;
import ca.glotov.expresspossess.expressions.ExpressionView;
import ca.glotov.expresspossess.groups.GroupDetail;
import ca.glotov.expresspossess.groups.GroupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * The fictional family on the demo instance. Wipes every table and rebuilds the same
 * scene on startup and every night, so reviewers always find the wishes in a state that
 * shows the whole lifecycle: one untaken, one in process by a hidden helper, one provided,
 * one received.
 *
 * <p>Goes through the ordinary services, so the seeded actions also produce the
 * notifications and outbox events a real family would.
 */
@Configuration
@EnableConfigurationProperties(DemoProperties.class)
public class DemoSeeder {

    public static final List<String> PERSONAS = List.of("Alice", "Bob", "Carol");
    static final String EMAIL_DOMAIN = "@demo.expresspossess.local";

    private static final Logger log = LoggerFactory.getLogger(DemoSeeder.class);

    private final DemoProperties demo;
    private final JdbcTemplate jdbc;
    private final AccountService accounts;
    private final UserRepository users;
    private final GroupService groups;
    private final ExpressionService expressions;

    DemoSeeder(DemoProperties demo, JdbcTemplate jdbc, AccountService accounts, UserRepository users,
               GroupService groups, ExpressionService expressions) {
        this.demo = demo;
        this.jdbc = jdbc;
        this.accounts = accounts;
        this.users = users;
        this.groups = groups;
        this.expressions = expressions;
    }

    public boolean enabled() {
        return demo.enabled();
    }

    public static String emailOf(String persona) {
        return persona.toLowerCase() + EMAIL_DOMAIN;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        if (demo.enabled()) {
            reset();
        }
    }

    @Scheduled(cron = "${app.demo.reset-cron}")
    public void nightly() {
        if (demo.enabled()) {
            reset();
        }
    }

    @Transactional
    public void reset() {
        log.info("Demo reset: wiping all data and seeding the fictional family");
        jdbc.execute("truncate table notifications, outbox_events, comments, expressions, group_invitations, "
                + "group_members, groups, password_reset_tokens, users restart identity cascade");

        User alice = accounts.register(emailOf("Alice"), demo.password(), "Alice");
        alice.setRole(Role.ADMIN);
        users.save(alice);
        User bob = accounts.register(emailOf("Bob"), demo.password(), "Bob");
        User carol = accounts.register(emailOf("Carol"), demo.password(), "Carol");

        GroupDetail family = groups.create("The Bakers", alice.getId());
        groups.invite(family.id(), alice.getId(), bob.getEmail());
        groups.invite(family.id(), alice.getId(), carol.getEmail());
        long g = family.id();

        // Carol's wish, nobody has taken it: shows the flashing mark on My Groups.
        expressions.create(g, carol.getId(),
                "Over-ear headphones, noise cancelling\nhttps://www.example.com/headphones/quiet-40",
                LocalDate.now().plusDays(28));

        // Alice's wish, Bob is on it incognito: shows "Incognito" and "Anonymous helper".
        ExpressionView kindle = expressions.create(g, alice.getId(),
                "Kindle case, a blue fabric one like this: https://www.example.com/kindle-case",
                LocalDate.now().plusDays(60));
        expressions.takeCare(kindle.id(), bob.getId(), true);
        expressions.comment(kindle.id(), bob.getId(), "Does it have to be blue?");
        expressions.comment(kindle.id(), alice.getId(), "Any dark colour is fine, thank you!");

        // Bob's wish, Carol has provided it: waiting for Bob's "Got it".
        ExpressionView pump = expressions.create(g, bob.getId(), "Bicycle pump with a pressure gauge", null);
        ExpressionView taken = expressions.takeCare(pump.id(), carol.getId(), false);
        expressions.editCare(pump.id(), carol.getId(), false, LocalDate.now().plusDays(3), true, taken.version());

        // Carol's older wish, received: read-only, comments closed.
        ExpressionView socks = expressions.create(g, carol.getId(), "Wool hiking socks, size 39", null);
        ExpressionView socksTaken = expressions.takeCare(socks.id(), alice.getId(), false);
        expressions.editCare(socks.id(), alice.getId(), false, null, true, socksTaken.version());
        expressions.markReceived(socks.id(), carol.getId());

        groups.create("Book club", bob.getId());
        log.info("Demo reset done: {} personas, password '{}'", PERSONAS.size(), demo.password());
    }
}
