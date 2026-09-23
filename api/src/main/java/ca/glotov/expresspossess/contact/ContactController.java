package ca.glotov.expresspossess.contact;

import ca.glotov.expresspossess.auth.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Contact us, for logged-in members. The administrators read the messages on the administration page. */
@RestController
class ContactController {

    private final ContactService contact;

    ContactController(ContactService contact) {
        this.contact = contact;
    }

    record NewMessage(@NotNull ContactTopic topic, @NotBlank @Size(max = 2000) String body,
                      @Size(max = 500) String page) {
    }

    @PostMapping("/api/contact")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void send(@AuthenticationPrincipal AuthenticatedUser me, @Valid @RequestBody NewMessage body) {
        contact.send(me.getId(), body.topic(), body.body(), body.page());
    }
}
