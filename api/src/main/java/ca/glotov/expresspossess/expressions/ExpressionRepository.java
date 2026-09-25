package ca.glotov.expresspossess.expressions;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public interface ExpressionRepository extends JpaRepository<Expression, Long> {

    /**
     * The Take Care claim. One statement the database executes atomically: it only matches
     * while nobody has claimed the expression, so of any number of concurrent callers exactly
     * one changes a row. The caller treats 0 rows as "somebody else won".
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Expression e set e.implementerId = :userId, e.incognito = :incognito, "
            + "e.status = ca.glotov.expresspossess.expressions.ExpressionStatus.IN_PROCESS, "
            + "e.version = e.version + 1, e.updatedAt = :now "
            + "where e.id = :id and e.implementerId is null "
            + "and e.status = ca.glotov.expresspossess.expressions.ExpressionStatus.EXPRESSED")
    int claim(@Param("id") Long id, @Param("userId") Long userId, @Param("incognito") boolean incognito,
              @Param("now") Instant now);

    /**
     * Stores the picture taken from a link. Leaves the version alone, and changes nothing if
     * the creator uploaded a picture meanwhile or the link has left the description.
     */
    @Modifying
    @Transactional
    @Query("update Expression e set e.pictureUrl = :url, e.pictureLink = :link, "
            + "e.pictureAttempts = 0, e.pictureTriedAt = :now "
            + "where e.id = :id and (e.pictureUrl is null or e.pictureLink is not null) "
            + "and locate(:link, e.description) > 0")
    int setLinkPicture(@Param("id") Long id, @Param("url") String url, @Param("link") String link,
                       @Param("now") Instant now);

    /**
     * Records that the link gave no picture this time, counting the tries for the same link,
     * under the same conditions as {@link #setLinkPicture}. A picture from an earlier link goes.
     */
    @Modifying
    @Transactional
    @Query("update Expression e set e.pictureUrl = null, "
            + "e.pictureAttempts = case when e.pictureLink = :link then e.pictureAttempts + 1 else 1 end, "
            + "e.pictureLink = :link, e.pictureTriedAt = :now "
            + "where e.id = :id and (e.pictureUrl is null or e.pictureLink is not null) "
            + "and locate(:link, e.description) > 0")
    int recordLinkPictureFailure(@Param("id") Long id, @Param("link") String link, @Param("now") Instant now);

    @Modifying
    @Transactional
    @Query("update Expression e set e.pictureUrl = null, e.pictureLink = null, e.pictureAttempts = 0, "
            + "e.pictureTriedAt = null where e.id = :id and e.pictureLink is not null")
    int clearLinkPicture(@Param("id") Long id);

    /** Wishes whose link has given no picture yet, with tries left. */
    List<Expression> findByPictureUrlIsNullAndPictureLinkIsNotNullAndPictureAttemptsBetween(int from, int to);

    @Query("select e.id from Expression e where e.pictureUrl is null and e.pictureLink is null "
            + "and (e.description like '%http://%' or e.description like '%https://%')")
    List<Long> findIdsWaitingForLinkPicture();

    List<Expression> findByGroupIdOrderByCreatedAtDesc(Long groupId);

    List<Expression> findByGroupIdAndCreatorIdOrderByCreatedAtDesc(Long groupId, Long creatorId);

    List<Expression> findByGroupIdAndImplementerIdOrderByCreatedAtDesc(Long groupId, Long implementerId);

    List<Expression> findByGroupIdAndStatusOrderByCreatedAtDesc(Long groupId, ExpressionStatus status);

    List<Expression> findByGroupIdAndImplementerIdAndStatus(Long groupId, Long implementerId, ExpressionStatus status);

    boolean existsByGroupId(Long groupId);

    boolean existsByGroupIdAndStatus(Long groupId, ExpressionStatus status);

    boolean existsByGroupIdAndImplementerIdAndStatus(Long groupId, Long implementerId, ExpressionStatus status);

    @Query("select distinct e.creatorId from Expression e where e.groupId = :groupId")
    Set<Long> creatorIdsIn(@Param("groupId") Long groupId);
}
