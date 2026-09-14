package ca.glotov.expresspossess.expressions;

import java.util.List;

/** The Group activity page: three lists. Rows carry no comments. */
public record ActivityView(Long groupId,
                           String groupName,
                           List<ExpressionView> myExpressions,
                           List<ExpressionView> myImplementations,
                           List<ExpressionView> notTaken) {
}
