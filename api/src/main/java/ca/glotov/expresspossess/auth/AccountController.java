package ca.glotov.expresspossess.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The logged-in member's own account: the profile page in the spec.
 */
@RestController
@RequestMapping("/api/me")
class AccountController {

    private final AccountService accounts;

    AccountController(AccountService accounts) {
        this.accounts = accounts;
    }

    record UpdateProfileRequest(@NotBlank @Size(max = 100) String name, boolean emailEnabled) {
    }

    record ChangePasswordRequest(@NotBlank String currentPassword,
                                 @NotBlank @Size(min = 8, max = 72) String newPassword) {
    }

    @GetMapping
    UserResponse me(@AuthenticationPrincipal AuthenticatedUser me) {
        return UserResponse.of(accounts.get(me.getId()));
    }

    @PutMapping
    UserResponse updateProfile(@AuthenticationPrincipal AuthenticatedUser me,
                               @Valid @RequestBody UpdateProfileRequest body) {
        return UserResponse.of(accounts.updateProfile(me.getId(), body.name(), body.emailEnabled()));
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void changePassword(@AuthenticationPrincipal AuthenticatedUser me,
                        @Valid @RequestBody ChangePasswordRequest body) {
        accounts.changePassword(me.getId(), body.currentPassword(), body.newPassword());
    }
}
