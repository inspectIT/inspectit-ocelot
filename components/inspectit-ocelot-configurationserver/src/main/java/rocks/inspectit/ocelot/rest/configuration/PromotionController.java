package rocks.inspectit.ocelot.rest.configuration;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.error.ApiError;
import rocks.inspectit.ocelot.error.exceptions.SelfPromotionNotAllowedException;
import rocks.inspectit.ocelot.file.FileManager;
import rocks.inspectit.ocelot.file.versioning.model.ConfigurationPromotion;
import rocks.inspectit.ocelot.file.versioning.model.WorkspaceDiff;
import rocks.inspectit.ocelot.rest.AbstractBaseController;
import rocks.inspectit.ocelot.security.config.UserRoleConfiguration;

import java.io.IOException;
import java.util.ConcurrentModificationException;

/**
 * Controller for handling all promotion endpoints.
 */
@RestController
@Slf4j
public class PromotionController extends AbstractBaseController {

    @Autowired
    private InspectitServerSettings settings;

    @Autowired
    private FileManager fileManager;

    @ExceptionHandler({SelfPromotionNotAllowedException.class})
    public ResponseEntity<Object> handleSelfPromotion(Exception exception) {
        ApiError apiError = new ApiError(HttpStatus.FORBIDDEN, "Self-promotion not allowed", exception.getLocalizedMessage());
        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }

    @ApiOperation(value = "Fetch promotion files", notes = "Fetches all configuration files which are ready for promotion.")
    @GetMapping(value = "configuration/promotions")
    public WorkspaceDiff getPromotions(
            @ApiParam("Specifies whether the old and new content of each files should also be returned.")
            @RequestParam(defaultValue = "false", name = "include-content")
                    boolean includeContent, Authentication authentication) throws IOException, GitAPIException {
        WorkspaceDiff workspaceDiff = fileManager.getWorkspaceDiff(includeContent);
        workspaceDiff.setCanPromoteOwnChanges(allowSelfPromotion(authentication));
        return workspaceDiff;
    }

    @Secured(UserRoleConfiguration.PROMOTE_ACCESS_ROLE)
    @ApiOperation(value = "Promote configurations", notes = "Promotes the specified configuration files.")
    @PostMapping(value = "configuration/promote")
    public ResponseEntity promoteConfiguration(
            @ApiParam("The definition that contains the information about which files to promote.")
            @RequestBody
                    ConfigurationPromotion promotion, Authentication authentication) throws GitAPIException {
        boolean allowSelfPromotion = allowSelfPromotion(authentication);
        if (promotion.getCommitMessage() == null || promotion.getCommitMessage().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            fileManager.promoteConfiguration(promotion, allowSelfPromotion);
            return ResponseEntity.ok().build();
        } catch (ConcurrentModificationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    private boolean allowSelfPromotion(Authentication authentication) {
        if (!settings.getSecurity().isFourEyesPromotion()) {
            return true;
        }
        return authentication.getAuthorities()
                .stream()
                .anyMatch(auth -> auth.getAuthority().equals(UserRoleConfiguration.ADMIN_ACCESS_ROLE));
    }
}
