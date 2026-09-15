package ca.glotov.expresspossess.expressions;

import java.time.LocalDate;
import java.util.List;

/**
 * An expression as the viewer is allowed to see it. The {@code can*} flags are what the
 * page uses to show or hide controls; the server enforces the same rules again on every
 * call. {@code incognito} is only reported truthfully to the implementer and to a system
 * administrator; everyone else sees false and an implementer named "Incognito".
 */
public record ExpressionView(Long id,
                             Long groupId,
                             PersonRef creator,
                             PersonRef implementer,
                             ExpressionStatus status,
                             String description,
                             List<String> links,
                             String pictureUrl,
                             LocalDate wantedBy,
                             LocalDate providingBy,
                             boolean incognito,
                             long version,
                             boolean canEditWish,
                             boolean canEditCare,
                             boolean canTakeCare,
                             boolean canRelease,
                             boolean canDelete,
                             boolean canMarkReceived,
                             boolean commentsOpen,
                             List<CommentView> comments) {
}
