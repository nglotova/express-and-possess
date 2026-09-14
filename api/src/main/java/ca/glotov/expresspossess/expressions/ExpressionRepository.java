package ca.glotov.expresspossess.expressions;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
