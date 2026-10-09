package ca.glotov.expresspossess.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(ApiException.class)
    ProblemDetail handle(ApiException e) {
        return ProblemDetail.forStatusAndDetail(e.status(), e.getMessage());
    }

    /**
     * Two saves of the same row at the same instant: the version column lets only the first
     * through. The pages' own version checks catch the slower cases with the same message.
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail handle(ObjectOptimisticLockingFailureException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "This was changed by someone else at the same moment; reload and try again");
    }
}
