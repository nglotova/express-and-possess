package ca.glotov.expresspossess.expressions;

import ca.glotov.expresspossess.auth.AuthenticatedUser;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
class ExpressionController {

    private final ExpressionService expressions;
    private final FileStorage files;

    ExpressionController(ExpressionService expressions, FileStorage files) {
        this.expressions = expressions;
        this.files = files;
    }

    record CreateRequest(@NotBlank @Size(max = 4000) String description, LocalDate wantedBy) {
    }

    record WishRequest(@NotBlank @Size(max = 4000) String description, LocalDate wantedBy, long version) {
    }

    record CareRequest(boolean incognito, LocalDate providingBy, boolean provided, long version) {
    }

    record CommentRequest(@NotBlank @Size(max = 2000) String body) {
    }

    @GetMapping("/groups/{groupId}/activity")
    ActivityView activity(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long groupId) {
        return expressions.activity(groupId, me.getId());
    }

    @GetMapping("/groups/{groupId}/members/{userId}/expressions")
    List<ExpressionView> byMember(@AuthenticationPrincipal AuthenticatedUser me,
                                  @PathVariable Long groupId, @PathVariable Long userId) {
        return expressions.listByCreator(groupId, userId, me.getId());
    }

    @PostMapping("/groups/{groupId}/expressions")
    @ResponseStatus(HttpStatus.CREATED)
    ExpressionView create(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long groupId,
                          @Valid @RequestBody CreateRequest body) {
        return expressions.create(groupId, me.getId(), body.description(), body.wantedBy());
    }

    @GetMapping("/expressions/{id}")
    ExpressionView get(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long id) {
        return expressions.get(id, me.getId());
    }

    @PutMapping("/expressions/{id}/wish")
    ExpressionView editWish(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long id,
                            @Valid @RequestBody WishRequest body) {
        return expressions.editWish(id, me.getId(), body.description(), body.wantedBy(), body.version());
    }

    @PostMapping("/expressions/{id}/received")
    ExpressionView received(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long id) {
        return expressions.markReceived(id, me.getId());
    }

    @DeleteMapping("/expressions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long id) {
        expressions.delete(id, me.getId());
    }

    @PostMapping("/expressions/{id}/picture")
    ExpressionView picture(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long id,
                           @RequestParam("file") MultipartFile file) {
        return expressions.setPicture(id, me.getId(), files.store(file));
    }

    @PostMapping("/expressions/{id}/take-care")
    ExpressionView takeCare(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long id) {
        return expressions.takeCare(id, me.getId());
    }

    @PutMapping("/expressions/{id}/care")
    ExpressionView editCare(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long id,
                            @Valid @RequestBody CareRequest body) {
        return expressions.editCare(id, me.getId(), body.incognito(), body.providingBy(), body.provided(), body.version());
    }

    @PostMapping("/expressions/{id}/release")
    ExpressionView release(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long id) {
        return expressions.release(id, me.getId());
    }

    @PostMapping("/expressions/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    ExpressionView comment(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long id,
                           @Valid @RequestBody CommentRequest body) {
        return expressions.comment(id, me.getId(), body.body());
    }
}
