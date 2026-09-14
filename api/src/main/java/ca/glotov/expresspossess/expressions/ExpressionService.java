package ca.glotov.expresspossess.expressions;

import ca.glotov.expresspossess.auth.Role;
import ca.glotov.expresspossess.auth.User;
import ca.glotov.expresspossess.auth.UserRepository;
import ca.glotov.expresspossess.common.ApiException;
import ca.glotov.expresspossess.groups.Group;
import ca.glotov.expresspossess.groups.GroupService;
import ca.glotov.expresspossess.groups.MemberLeftEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ca.glotov.expresspossess.expressions.ExpressionStatus.EXPRESSED;
import static ca.glotov.expresspossess.expressions.ExpressionStatus.IN_POSSESSION;
import static ca.glotov.expresspossess.expressions.ExpressionStatus.IN_PROCESS;
import static ca.glotov.expresspossess.expressions.ExpressionStatus.PROVIDED;

/**
 * Every rule about a wish lives here. Each public method is one row of the transition
 * table in the spec, or one of the reads that feed a page.
 */
@Service
@Transactional
public class ExpressionService {

    private final ExpressionRepository expressions;
    private final CommentRepository comments;
    private final UserRepository users;
    private final GroupService groups;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    ExpressionService(ExpressionRepository expressions,
                      CommentRepository comments,
                      UserRepository users,
                      GroupService groups,
                      ApplicationEventPublisher events,
                      Clock clock) {
        this.expressions = expressions;
        this.comments = comments;
        this.users = users;
        this.groups = groups;
        this.events = events;
        this.clock = clock;
    }

    // ---- reads ---------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ActivityView activity(Long groupId, Long viewerId) {
        groups.memberOf(groupId, viewerId);
        String groupName = groups.get(groupId, viewerId).name();
        Viewer viewer = viewer(viewerId);
        return new ActivityView(groupId, groupName,
                rows(expressions.findByGroupIdAndCreatorIdOrderByCreatedAtDesc(groupId, viewerId), viewer),
                rows(expressions.findByGroupIdAndImplementerIdOrderByCreatedAtDesc(groupId, viewerId), viewer),
                rows(expressions.findByGroupIdAndStatusOrderByCreatedAtDesc(groupId, EXPRESSED).stream()
                        .filter(e -> !e.isCreator(viewerId)).toList(), viewer));
    }

    @Transactional(readOnly = true)
    public List<ExpressionView> listByCreator(Long groupId, Long creatorId, Long viewerId) {
        groups.memberOf(groupId, viewerId);
        return rows(expressions.findByGroupIdAndCreatorIdOrderByCreatedAtDesc(groupId, creatorId), viewer(viewerId));
    }

    @Transactional(readOnly = true)
    public ExpressionView get(Long id, Long viewerId) {
        Expression expression = visible(id, viewerId);
        return view(expression, viewer(viewerId), comments.findByExpressionIdOrderByCreatedAt(id));
    }

    // ---- the creator's section -----------------------------------------------------

    public ExpressionView create(Long groupId, Long creatorId, String description, LocalDate wantedBy) {
        Group group = groups.activeGroupFor(groupId, creatorId);
        Expression expression = expressions.save(new Expression(group.getId(), creatorId, description.trim(), wantedBy));
        events.publishEvent(new ExpressionChanged(ExpressionChanged.Type.CREATED, expression.getId(), groupId, creatorId));
        return changed(expression.getId(), creatorId);
    }

    /**
     * The creator's Submit. The description can only change while nobody has taken care;
     * the date can change until the wish is received. Version guards against overwriting
     * someone else's edit.
     */
    public ExpressionView editWish(Long id, Long userId, String description, LocalDate wantedBy, long version) {
        Expression expression = editable(id, userId);
        requireCreator(expression, userId);
        requireVersion(expression, version);
        if (!expression.is(EXPRESSED) && !expression.getDescription().equals(description.trim())) {
            throw ApiException.conflict("The description is locked while someone takes care of this wish");
        }
        expression.editWish(description.trim(), wantedBy);
        return changed(id, userId);
    }

    /** The creator ticks Got it. */
    public ExpressionView markReceived(Long id, Long userId) {
        Expression expression = editable(id, userId);
        requireCreator(expression, userId);
        requireStatus(expression, PROVIDED, "Only a provided wish can be marked as received");
        expression.markReceived();
        events.publishEvent(new ExpressionChanged(ExpressionChanged.Type.RECEIVED, id, expression.getGroupId(), userId));
        return changed(id, userId);
    }

    public void delete(Long id, Long userId) {
        Expression expression = editable(id, userId);
        requireCreator(expression, userId);
        if (expression.is(PROVIDED)) {
            throw ApiException.conflict("A provided wish cannot be deleted; mark it as received instead");
        }
        events.publishEvent(new ExpressionChanged(ExpressionChanged.Type.DELETED, id, expression.getGroupId(), userId));
        expressions.delete(expression);
    }

    public ExpressionView setPicture(Long id, Long userId, String pictureUrl) {
        Expression expression = editable(id, userId);
        requireCreator(expression, userId);
        expression.setPicture(pictureUrl);
        return changed(id, userId);
    }

    // ---- the implementer's section -------------------------------------------------

    /**
     * Take Care. The claim is a single conditional update, so two members pressing the
     * button at the same moment cannot both win: the database changes one row for exactly
     * one of them and the other gets 0 rows, reported here as 409.
     */
    public ExpressionView takeCare(Long id, Long userId) {
        Expression expression = editable(id, userId);
        if (expression.isCreator(userId)) {
            throw ApiException.forbidden("You cannot take care of your own wish");
        }
        int claimed = expressions.claim(id, userId, clock.instant());
        if (claimed == 0) {
            throw ApiException.conflict("Someone else has already taken care of this wish");
        }
        events.publishEvent(new ExpressionChanged(ExpressionChanged.Type.TAKEN, id, expression.getGroupId(), userId));
        return changed(id, userId);
    }

    /** The implementer's Submit: Incognito, the providing-by date, and optionally Provided. */
    public ExpressionView editCare(Long id, Long userId, boolean incognito, LocalDate providingBy,
                                   boolean provided, long version) {
        Expression expression = editable(id, userId);
        requireImplementer(expression, userId);
        requireStatus(expression, IN_PROCESS, "This wish is no longer in process");
        requireVersion(expression, version);
        expression.editCare(incognito, providingBy);
        if (provided) {
            expression.markProvided();
            events.publishEvent(new ExpressionChanged(ExpressionChanged.Type.PROVIDED, id, expression.getGroupId(), userId));
        }
        return changed(id, userId);
    }

    public ExpressionView release(Long id, Long userId) {
        Expression expression = editable(id, userId);
        requireImplementer(expression, userId);
        requireStatus(expression, IN_PROCESS, "Only a wish in process can be released");
        expression.release();
        events.publishEvent(new ExpressionChanged(ExpressionChanged.Type.RELEASED, id, expression.getGroupId(), userId));
        return changed(id, userId);
    }

    /** A member who leaves or is removed drops everything they were implementing. */
    @EventListener
    public void onMemberLeft(MemberLeftEvent event) {
        for (Expression expression : expressions.findByGroupIdAndImplementerIdAndStatus(
                event.groupId(), event.userId(), IN_PROCESS)) {
            expression.release();
            events.publishEvent(new ExpressionChanged(ExpressionChanged.Type.RELEASED,
                    expression.getId(), event.groupId(), event.userId()));
        }
    }

    // ---- comments ------------------------------------------------------------------

    public ExpressionView comment(Long id, Long userId, String body) {
        Expression expression = visible(id, userId);
        if (expression.is(IN_POSSESSION)) {
            throw ApiException.conflict("Comments are closed once the wish is received");
        }
        comments.save(new Comment(id, userId, body.trim()));
        events.publishEvent(new ExpressionChanged(ExpressionChanged.Type.COMMENTED, id, expression.getGroupId(), userId));
        return changed(id, userId);
    }

    // ---- building views ------------------------------------------------------------

    /**
     * The view returned after a change. Flushing first makes Hibernate bump the version
     * column now, so the number the client gets back is the one it must send with its next
     * edit.
     */
    private ExpressionView changed(Long id, Long userId) {
        expressions.flush();
        return get(id, userId);
    }

    private record Viewer(Long id, boolean systemAdmin) {
    }

    private Viewer viewer(Long userId) {
        boolean admin = users.findById(userId).map(u -> u.getRole() == Role.ADMIN).orElse(false);
        return new Viewer(userId, admin);
    }

    private List<ExpressionView> rows(List<Expression> list, Viewer viewer) {
        return list.stream().map(e -> view(e, viewer, List.of())).toList();
    }

    private ExpressionView view(Expression e, Viewer viewer, List<Comment> commentList) {
        Set<Long> ids = Stream.concat(
                        Stream.of(e.getCreatorId(), e.getImplementerId()),
                        commentList.stream().map(Comment::getAuthorId))
                .filter(id -> id != null).collect(Collectors.toSet());
        Map<Long, String> names = users.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, User::getName));

        boolean seesImplementer = e.isImplementer(viewer.id()) || viewer.systemAdmin() || !e.isIncognito();
        PersonRef implementer = e.getImplementerId() == null ? null
                : seesImplementer ? new PersonRef(e.getImplementerId(), names.get(e.getImplementerId()))
                : PersonRef.INCOGNITO;

        boolean open = !e.is(IN_POSSESSION);
        boolean creator = e.isCreator(viewer.id());
        boolean implementing = e.isImplementer(viewer.id());

        return new ExpressionView(
                e.getId(), e.getGroupId(),
                new PersonRef(e.getCreatorId(), names.get(e.getCreatorId())),
                implementer,
                e.getStatus(), e.getDescription(), e.getPictureUrl(), e.getWantedBy(), e.getProvidingBy(),
                seesImplementer && e.isIncognito(),
                e.getVersion(),
                open && creator,
                open && implementing && e.is(IN_PROCESS),
                open && !creator && e.is(EXPRESSED),
                open && implementing && e.is(IN_PROCESS),
                open && creator && !e.is(PROVIDED),
                open && creator && e.is(PROVIDED),
                open,
                commentList.stream().map(c -> new CommentView(
                        c.getId(),
                        c.isSystemNote() ? null
                                : c.getAuthorId().equals(e.getImplementerId()) && !seesImplementer
                                ? PersonRef.ANONYMOUS_HELPER
                                : new PersonRef(c.getAuthorId(), names.get(c.getAuthorId())),
                        c.getBody(), c.isSystemNote(), c.getCreatedAt())).toList());
    }

    // ---- guards --------------------------------------------------------------------

    /** The expression, if the viewer belongs to its group. Otherwise 404. */
    private Expression visible(Long id, Long userId) {
        Expression expression = expressions.findById(id)
                .orElseThrow(() -> ApiException.notFound("No such expression"));
        groups.memberOf(expression.getGroupId(), userId);
        return expression;
    }

    /** Visible, in an active group, and not yet received. */
    private Expression editable(Long id, Long userId) {
        Expression expression = visible(id, userId);
        groups.activeGroupFor(expression.getGroupId(), userId);
        if (expression.is(IN_POSSESSION)) {
            throw ApiException.conflict("This wish has been received and can no longer be changed");
        }
        return expression;
    }

    private static void requireCreator(Expression expression, Long userId) {
        if (!expression.isCreator(userId)) {
            throw ApiException.forbidden("Only the wish creator can do this");
        }
    }

    private static void requireImplementer(Expression expression, Long userId) {
        if (!expression.isImplementer(userId)) {
            throw ApiException.forbidden("Only the implementer can do this");
        }
    }

    private static void requireStatus(Expression expression, ExpressionStatus status, String message) {
        if (!expression.is(status)) {
            throw ApiException.conflict(message);
        }
    }

    private static void requireVersion(Expression expression, long version) {
        if (expression.getVersion() != version) {
            throw ApiException.conflict("This wish was changed by someone else; reload and try again");
        }
    }
}
