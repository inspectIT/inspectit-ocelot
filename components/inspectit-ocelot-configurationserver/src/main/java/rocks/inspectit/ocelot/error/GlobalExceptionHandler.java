package rocks.inspectit.ocelot.error;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import rocks.inspectit.ocelot.error.exceptions.NotSupportedWithLdapException;

/**
 * The global exception handler.
 */
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler({NotSupportedWithLdapException.class})
    public ResponseEntity<Object> handleNotSupportedWithLdapException(Exception exception, WebRequest request) {
        ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST, "Endpoint is not supported in the current configuration.", exception.getLocalizedMessage());
        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }

    @ExceptionHandler({Exception.class})
    public ResponseEntity<Object> handleAll(Exception exception, WebRequest request) {
        ApiError apiError = new ApiError(HttpStatus.INTERNAL_SERVER_ERROR, exception);
        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }
}
