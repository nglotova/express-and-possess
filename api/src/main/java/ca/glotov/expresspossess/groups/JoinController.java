package ca.glotov.expresspossess.groups;

import ca.glotov.expresspossess.auth.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The two ways into a group from a link: an emailed invitation and a share link. The GET
 * endpoints are public so the page can show the group name before the person registers;
 * the POST endpoints need a logged-in user.
 */
@RestController
@RequestMapping("/api")
class JoinController {

    private final GroupService groups;

    JoinController(GroupService groups) {
        this.groups = groups;
    }

    record JoinedResponse(Long groupId) {
    }

    record ShareInfo(String groupName) {
    }

    @GetMapping("/invitations/{token}")
    GroupService.InvitationInfo invitation(@PathVariable String token) {
        return groups.invitationInfo(token);
    }

    @PostMapping("/invitations/{token}/accept")
    JoinedResponse accept(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable String token) {
        return new JoinedResponse(groups.acceptInvitation(token, me.getId()));
    }

    @GetMapping("/join/{token}")
    ShareInfo shareInfo(@PathVariable String token) {
        return new ShareInfo(groups.shareGroupName(token));
    }

    @PostMapping("/join/{token}")
    JoinedResponse join(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable String token) {
        return new JoinedResponse(groups.joinByShareLink(token, me.getId()));
    }
}
