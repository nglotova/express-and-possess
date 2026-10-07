package ca.glotov.expresspossess.expressions;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByExpressionIdOrderByCreatedAt(Long expressionId);

    /** Everyone who wrote a comment on the expression; system notes have no author. */
    @Query("select distinct c.authorId from Comment c where c.expressionId = :expressionId and c.authorId is not null")
    List<Long> findAuthorIds(@Param("expressionId") Long expressionId);
}
