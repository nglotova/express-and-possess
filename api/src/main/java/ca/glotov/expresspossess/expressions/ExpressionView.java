package ca.glotov.expresspossess.expressions;

import java.time.LocalDate;
import java.util.List;

/**
 * An expression as the viewer is allowed to see it. The {@code can*} flags are what the
 * page uses to show or hide controls; the server enforces the same rules again on every
 * call. {@code incognito} is only reported truthfully to the implementer and to a site
 * administrator; everyone else sees false and an implementer named "Incognito".
 * {@code canManage} is for the site administrator and for the admin of an open group: any
 * status, and delete, whatever the wish's state.
 *
 * @param pictureFromLink the picture was taken from the first link in the description
 * @param picturePending  a picture is being fetched from that link; the page asks again until it ends
 */
public record ExpressionView(Long id,
                             Long groupId,
                             PersonRef creator,
                             PersonRef implementer,
                             ExpressionStatus status,
                             String description,
                             String pictureUrl,
                             boolean pictureFromLink,
                             boolean picturePending,
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
                             boolean canManage,
                             boolean commentsOpen,
                             List<CommentView> comments) {
}
