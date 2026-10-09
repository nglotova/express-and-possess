package ca.glotov.expresspossess.common;

import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {

    @Test
    void twoSavesAtTheSameInstantAnswerConflictInsteadOfAServerError() {
        ProblemDetail problem = new ApiExceptionHandler()
                .handle(new ObjectOptimisticLockingFailureException("Expression", 7L));

        assertThat(problem.getStatus()).isEqualTo(409);
        assertThat(problem.getDetail()).isEqualTo("This was changed by someone else at the same moment; reload and try again");
    }
}
