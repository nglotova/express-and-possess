package ca.glotov.expresspossess.notifications;

import ca.glotov.expresspossess.auth.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The bell in the header. */
@RestController
@RequestMapping("/api/notifications")
class NotificationController {

    private final NotificationService notifications;

    NotificationController(NotificationService notifications) {
        this.notifications = notifications;
    }

    record UnreadCount(long unread) {
    }

    @GetMapping
    NotificationService.Inbox inbox(@AuthenticationPrincipal AuthenticatedUser me) {
        return notifications.inbox(me.getId());
    }

    @GetMapping("/unread-count")
    UnreadCount unreadCount(@AuthenticationPrincipal AuthenticatedUser me) {
        return new UnreadCount(notifications.unreadCount(me.getId()));
    }

    @PostMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void markRead(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long id) {
        notifications.markRead(me.getId(), id);
    }

    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void markAllRead(@AuthenticationPrincipal AuthenticatedUser me) {
        notifications.markAllRead(me.getId());
    }
}
