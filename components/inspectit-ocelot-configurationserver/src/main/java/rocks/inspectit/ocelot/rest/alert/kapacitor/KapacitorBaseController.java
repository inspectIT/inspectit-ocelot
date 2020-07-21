package rocks.inspectit.ocelot.rest.alert.kapacitor;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.config.model.KapacitorSettings;
import rocks.inspectit.ocelot.error.ApiError;
import rocks.inspectit.ocelot.rest.AbstractBaseController;
import rocks.inspectit.ocelot.rest.alert.kapacitor.exceptions.KapacitorNotEnabledException;
import rocks.inspectit.ocelot.rest.alert.kapacitor.exceptions.KapacitorServerException;

/**
 * Base class for all Kapacitor related controllers.
 * Takes care of initializing the rest-template.
 */
public class KapacitorBaseController extends AbstractBaseController {

    @VisibleForTesting
    RestTemplate kapacitorRestTemplate;

    @Autowired
    public KapacitorBaseController(InspectitServerSettings settings) {
        KapacitorSettings kapacitorSettings = settings.getKapacitor();
        if (!StringUtils.isBlank(kapacitorSettings.getUrl())) {
            RestTemplateBuilder builder = new RestTemplateBuilder().rootUri(kapacitorSettings.getUrl());
            if (!StringUtils.isBlank(kapacitorSettings.getUsername())) {
                builder = builder.basicAuthentication(kapacitorSettings.getUsername(), kapacitorSettings.getPassword());
            }
            builder = builder.errorHandler(new KapacitorErrorHandler());
            kapacitorRestTemplate = builder.build();
        }
    }

    @ExceptionHandler
    private ResponseEntity<ApiError> handleKapacitorNotEnabled(KapacitorNotEnabledException exception) {
        ApiError apiError = new ApiError(HttpStatus.FAILED_DEPENDENCY, "Kapacitor connection is not configured.", exception
                .getLocalizedMessage());
        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }

    @ExceptionHandler
    private ResponseEntity<ApiError> handleKapacitorError(ResourceAccessException exception) {
        Throwable cause = exception.getCause();
        ApiError apiError;
        if (cause instanceof KapacitorServerException) {
            KapacitorServerException kapacitorCause = (KapacitorServerException) cause;
            apiError = new ApiError(kapacitorCause.getStatus(), kapacitorCause.getMessage(), "");
        } else {
            apiError = new ApiError(HttpStatus.FAILED_DEPENDENCY,
                    "Kapacitor is not reachable.", exception.getLocalizedMessage());
        }
        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }

    /**
     * Should be used by subclasses to issue requests to kapacitor.
     * If kapacitor has not been configured, this method will throw a {@link KapacitorNotEnabledException},
     * which is handled by this class.
     *
     * @return the RestTemplate which can be used to fire requests to kapacitor.
     */
    protected RestTemplate kapacitor() {
        if (kapacitorRestTemplate != null) {
            return kapacitorRestTemplate;
        }
        throw new KapacitorNotEnabledException();
    }

    protected boolean isKapacitorEnabled() {
        return kapacitorRestTemplate != null;
    }
}
