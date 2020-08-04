package rocks.inspectit.ocelot.rest.alert.kapacitor.exceptions;

import org.springframework.http.HttpStatus;

import java.io.IOException;

/**
 * When Kapacitor returns an error code, its message and status will be packed in this exception.
 */
public class KapacitorServerException extends IOException {

    private HttpStatus status;

    public KapacitorServerException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
