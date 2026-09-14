package ca.glotov.expresspossess.groups;

import ca.glotov.expresspossess.auth.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
class GroupController {

    private final GroupService groups;

    GroupController(GroupService groups) {
        this.groups = groups;
    }

    record NameRequest(@NotBlank @Size(max = 100) String name) {
    }

    record InviteRequest(@NotBlank @Email @Size(max = 254) String email) {
    }

    record InviteResponse(GroupService.InviteOutcome outcome) {
    }

    record ShareLinkRequest(boolean enabled) {
    }

    record ShareLinkResponse(boolean enabled, String link) {
    }

    @GetMapping
    List<GroupSummary> myGroups(@AuthenticationPrincipal AuthenticatedUser me) {
        return groups.listFor(me.getId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    GroupDetail create(@AuthenticationPrincipal AuthenticatedUser me, @Valid @RequestBody NameRequest body) {
        return groups.create(body.name(), me.getId());
    }

    @GetMapping("/{id}")
    GroupDetail get(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long id) {
        return groups.get(id, me.getId());
    }

    @PutMapping("/{id}")
    GroupDetail rename(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long id,
                       @Valid @RequestBody NameRequest body) {
        return groups.rename(id, me.getId(), body.name());
    }

    @PostMapping("/{id}/close")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void close(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long id) {
        groups.close(id, me.getId());
    }

    @PostMapping("/{id}/archive")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void archive(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long id) {
        groups.archive(id, me.getId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long id) {
        groups.delete(id, me.getId());
    }

    @PostMapping("/{id}/invitations")
    @ResponseStatus(HttpStatus.ACCEPTED)
    InviteResponse invite(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long id,
                          @Valid @RequestBody InviteRequest body) {
        return new InviteResponse(groups.invite(id, me.getId(), body.email()));
    }

    @DeleteMapping("/{id}/invitations/{invitationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void cancelInvitation(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long id,
                          @PathVariable Long invitationId) {
        groups.cancelInvitation(id, me.getId(), invitationId);
    }

    @DeleteMapping("/{id}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void remove(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long id, @PathVariable Long userId) {
        groups.remove(id, me.getId(), userId);
    }

    @PostMapping("/{id}/members/{userId}/make-admin")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void makeAdmin(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long id, @PathVariable Long userId) {
        groups.makeAdmin(id, me.getId(), userId);
    }

    @PostMapping("/{id}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void leave(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long id) {
        groups.leave(id, me.getId());
    }

    @PutMapping("/{id}/share-link")
    ShareLinkResponse shareLink(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long id,
                                @RequestBody ShareLinkRequest body) {
        String link = groups.setShareLink(id, me.getId(), body.enabled());
        return new ShareLinkResponse(link != null, link);
    }
}
