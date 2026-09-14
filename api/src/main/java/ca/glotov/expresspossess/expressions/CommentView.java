package ca.glotov.expresspossess.expressions;

import java.time.Instant;

public record CommentView(Long id, PersonRef author, String body, boolean systemNote, Instant createdAt) {
}
