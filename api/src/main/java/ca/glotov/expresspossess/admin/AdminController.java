package ca.glotov.expresspossess.admin;

import ca.glotov.expresspossess.auth.AccountService;
import ca.glotov.expresspossess.auth.AuthenticatedUser;
import ca.glotov.expresspossess.auth.Role;
import ca.glotov.expresspossess.auth.UserResponse;
import ca.glotov.expresspossess.contact.ContactMessageView;
import ca.glotov.expresspossess.contact.ContactService;
import ca.glotov.expresspossess.expressions.ExpressionService;
import ca.glotov.expresspossess.expressions.ExpressionView;
import ca.glotov.expresspossess.groups.Group;
import ca.glotov.expresspossess.groups.GroupService;
import ca.glotov.expresspossess.groups.GroupStatus;
import ca.glotov.expresspossess.groups.MemberView;
import ca.glotov.expresspossess.settings.SiteSettingsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The administration page, section 11 of the spec. Everything under {@code /api/admin} is
 * restricted to the ADMIN role in the security configuration; this class adds no further
 * checks beyond the two that protect an administrator from locking themself out.
 */
@RestController
@RequestMapping("/api/admin")
class AdminController {

    private final AccountService accounts;
    private final GroupService groups;
    private final ExpressionService expressions;
    private final SiteSettingsService settings;
    private final ContactService contact;

    AdminController(AccountService accounts, GroupService groups, ExpressionService expressions,
                    SiteSettingsService settings, ContactService contact) {
        this.accounts = accounts;
        this.groups = groups;
        this.expressions = expressions;
        this.settings = settings;
        this.contact = contact;
    }

    // ---- users ---------------------------------------------------------------------

    record UserUpdate(@NotNull Boolean enabled, @NotNull Role role) {
    }

    @GetMapping("/users")
    List<UserResponse> users(@RequestParam(defaultValue = "") String q) {
        return accounts.search(q).stream().map(UserResponse::of).toList();
    }

    @PutMapping("/users/{id}")
    UserResponse updateUser(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long id,
                            @Valid @RequestBody UserUpdate body) {
        accounts.setEnabled(id, me.getId(), body.enabled());
        return UserResponse.of(accounts.setRole(id, me.getId(), body.role()));
    }

    // ---- groups --------------------------------------------------------------------

    record AdminGroupDetail(Long id, String name, GroupStatus status, List<MemberView> members,
                            List<ExpressionView> expressions) {
    }

    @GetMapping("/groups")
    List<GroupService.AdminGroupRow> groups() {
        return groups.adminList();
    }

    @GetMapping("/groups/{id}")
    AdminGroupDetail group(@PathVariable Long id) {
        Group group = groups.adminGet(id);
        return new AdminGroupDetail(group.getId(), group.getName(), group.getStatus(),
                groups.adminMembers(id), expressions.adminListInGroup(id));
    }

    @PostMapping("/groups/{id}/restore")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void restore(@PathVariable Long id) {
        groups.restore(id);
    }

    @DeleteMapping("/groups/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteGroup(@PathVariable Long id) {
        groups.adminDelete(id);
    }

    // ---- expressions ---------------------------------------------------------------
    // Status changes and deletes go through /api/expressions/{id}/manage, shared with group admins.

    @GetMapping("/expressions/{id}")
    ExpressionView expression(@PathVariable Long id) {
        return expressions.adminGet(id);
    }

    // ---- site settings -------------------------------------------------------------

    record Settings(@NotNull @Min(1) @Max(1000) Integer invitationsPerDay) {
    }

    @GetMapping("/settings")
    Settings siteSettings() {
        return new Settings(settings.invitationsPerDay());
    }

    @PutMapping("/settings")
    Settings updateSiteSettings(@Valid @RequestBody Settings body) {
        settings.setInvitationsPerDay(body.invitationsPerDay());
        return siteSettings();
    }

    // ---- contact us ----------------------------------------------------------------

    @GetMapping("/messages")
    List<ContactMessageView> messages() {
        return contact.latest();
    }
}
