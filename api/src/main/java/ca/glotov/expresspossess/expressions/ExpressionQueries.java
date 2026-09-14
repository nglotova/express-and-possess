package ca.glotov.expresspossess.expressions;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

/**
 * Read-only facts about a group's expressions that other domains need: the groups domain
 * computes its status and attention flags from these. Kept apart from
 * {@link ExpressionService} so that the groups domain can depend on this class without the
 * two services depending on each other.
 */
@Service
@Transactional(readOnly = true)
public class ExpressionQueries {

    private final ExpressionRepository expressions;

    ExpressionQueries(ExpressionRepository expressions) {
        this.expressions = expressions;
    }

    /** "Working" in the spec: at least one expression was ever created. */
    public boolean groupHasExpressions(Long groupId) {
        return expressions.existsByGroupId(groupId);
    }

    /** The flashing "!" on My Groups: something nobody has taken care of yet. */
    public boolean groupHasUntaken(Long groupId) {
        return expressions.existsByGroupIdAndStatus(groupId, ExpressionStatus.EXPRESSED);
    }

    /** The warning sign on My Groups: the member is implementing something here. */
    public boolean isImplementingIn(Long groupId, Long userId) {
        return expressions.existsByGroupIdAndImplementerIdAndStatus(groupId, userId, ExpressionStatus.IN_PROCESS);
    }

    /** The mark on the Group users page. */
    public Set<Long> creatorsIn(Long groupId) {
        return expressions.creatorIdsIn(groupId);
    }

    /** What a notification needs to know about an expression. */
    public record Facts(Long id, Long groupId, Long creatorId, Long implementerId, boolean incognito,
                        ExpressionStatus status, String description) {
    }

    public Optional<Facts> facts(Long expressionId) {
        return expressions.findById(expressionId).map(e -> new Facts(e.getId(), e.getGroupId(), e.getCreatorId(),
                e.getImplementerId(), e.isIncognito(), e.getStatus(), e.getDescription()));
    }
}
