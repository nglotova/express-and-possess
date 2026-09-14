package ca.glotov.expresspossess.expressions;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByExpressionIdOrderByCreatedAt(Long expressionId);
}
