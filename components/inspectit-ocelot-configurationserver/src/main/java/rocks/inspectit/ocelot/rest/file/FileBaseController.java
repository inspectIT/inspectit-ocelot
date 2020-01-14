package rocks.inspectit.ocelot.rest.file;

import org.apache.commons.io.FileExistsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import rocks.inspectit.ocelot.file.manager.AbstractFileManager;
import rocks.inspectit.ocelot.file.manager.ConfigurationFileManager;
import rocks.inspectit.ocelot.rest.AbstractBaseController;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;

/**
 * Base class for file related controllers.
 * This base class performs the error handling.
 */
public class FileBaseController extends AbstractBaseController {

    @Autowired
    protected ConfigurationFileManager configurationFileManager;

    @ExceptionHandler({NoSuchFileException.class, FileNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void handleFileNotFound(IOException e) {
    }

    @ExceptionHandler({FileAlreadyExistsException.class, FileExistsException.class, NotDirectoryException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public void handleConflict(IOException e) {
    }

    @ExceptionHandler({AccessDeniedException.class, IOException.class})
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public void handleAccessDenied(IOException e) {
    }

}
