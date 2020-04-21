package rocks.inspectit.ocelot.file.accessor;

public class DirectoryTraversalException extends RuntimeException {

    public DirectoryTraversalException(String message) {
        super(message);
    }

    public DirectoryTraversalException(String message, Throwable cause) {
        super(message, cause);
    }
}
