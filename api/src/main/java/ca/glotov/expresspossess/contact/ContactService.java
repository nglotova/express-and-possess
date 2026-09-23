package ca.glotov.expresspossess.contact;

import ca.glotov.expresspossess.auth.AccountService;
import ca.glotov.expresspossess.auth.Role;
import ca.glotov.expresspossess.auth.User;
import ca.glotov.expresspossess.auth.UserRepository;
import ca.glotov.expresspossess.common.ApiException;
import ca.glotov.expresspossess.notifications.NotificationService;
import ca.glotov.expresspossess.notifications.NotificationType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Contact us: members write to the site administrators. Each message is kept for the
 * administration page and announced to every administrator under the bell and by email.
 */
@Service
@Transactional
public class ContactService {

    /** Enough for a real conversation, too few to flood the administrators. */
    static final int MESSAGES_PER_DAY = 5;

    private static final int PREVIEW_LENGTH = 120;

    private final ContactMessageRepository messages;
    private final AccountService accounts;
    private final UserRepository users;
    private final NotificationService notifications;
    private final Clock clock;

    ContactService(ContactMessageRepository messages, AccountService accounts, UserRepository users,
                   NotificationService notifications, Clock clock) {
        this.messages = messages;
        this.accounts = accounts;
        this.users = users;
        this.notifications = notifications;
        this.clock = clock;
    }

    public void send(Long senderId, ContactTopic topic, String body, String page) {
        Instant now = clock.instant();
        if (messages.countBySenderIdAndCreatedAtAfter(senderId, now.minus(Duration.ofDays(1))) >= MESSAGES_PER_DAY) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "You have sent " + MESSAGES_PER_DAY
                    + " messages in the last 24 hours, which is the limit. Please try again tomorrow.");
        }
        String text = body.trim();
        ContactMessage saved = messages.save(new ContactMessage(senderId, topic, text, page, now));

        User sender = accounts.get(senderId);
        List<Long> administrators = users.findByRoleAndEnabledTrue(Role.ADMIN).stream().map(User::getId).toList();
        notifications.notifyAdministrators(NotificationType.CONTACT_MESSAGE, administrators,
                sender.getName() + " wrote about " + describe(topic) + ": " + preview(text), saved.getId());
    }

    @Transactional(readOnly = true)
    public List<ContactMessageView> latest() {
        List<ContactMessage> latest = messages.findTop100ByOrderByCreatedAtDesc();
        Map<Long, User> senders = accounts.getAll(latest.stream().map(ContactMessage::getSenderId).toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        return latest.stream().map(m -> {
            User sender = senders.get(m.getSenderId());
            return new ContactMessageView(m.getId(), sender.getName(), sender.getEmail(), m.getTopic(), m.getBody(),
                    m.getPage(), m.getCreatedAt());
        }).toList();
    }

    private static String describe(ContactTopic topic) {
        return switch (topic) {
            case PROBLEM -> "a problem";
            case SUGGESTION -> "a suggestion";
            case OTHER -> "something else";
        };
    }

    private static String preview(String text) {
        String oneLine = text.replaceAll("\\s+", " ");
        return oneLine.length() <= PREVIEW_LENGTH ? oneLine : oneLine.substring(0, PREVIEW_LENGTH) + "…";
    }
}
