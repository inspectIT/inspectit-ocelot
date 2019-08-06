package rocks.inspectit.ocelot.error;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

/**
 * Unified API error class. This class should be used when returning an error.
 */
@Data
public class ApiError {

    /**
     * The HTTP status code of the response.
     */
    private HttpStatus status;

    /**
     * Human readable message which can be used in error dialogs etc.
     */
    private String message;

    /**
     * An error description which can be used for debugging purposes.
     */
    private String debugMessage;

    /**
     * The date when the exception occurred (current timestamp).
     */
    @Builder.Default
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    private LocalDateTime timestamp = LocalDateTime.now();

    public ApiError(HttpStatus status, Exception exception) {
        this(status, "Unexpected error", exception.getLocalizedMessage());
    }

    public ApiError(HttpStatus status, String message, String debugMessage) {
        this.status = status;
        this.message = message;
        this.debugMessage = debugMessage;
    }
}
