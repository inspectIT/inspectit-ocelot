package rocks.inspectit.ocelot.rest.file;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.HandlerMapping;
import rocks.inspectit.ocelot.file.FileManager;
import rocks.inspectit.ocelot.rest.AbstractBaseController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;

public class FileBaseController extends AbstractBaseController {

    @Autowired
    protected FileManager files;

    @ExceptionHandler({NoSuchFileException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void handleFileNotFound(NoSuchFileException e) {
    }

    @ExceptionHandler({FileAlreadyExistsException.class, NotDirectoryException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public void handleConflict(IOException e) {
    }

    @ExceptionHandler({AccessDeniedException.class})
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public void handleAccessDenied(AccessDeniedException e) {
    }

    protected static String getRequestSubPath(HttpServletRequest request) {
        String path =
                request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE).toString();
        String bestMatchingPattern =
                request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE).toString();
        return new AntPathMatcher().extractPathWithinPattern(bestMatchingPattern, path);
    }
}
