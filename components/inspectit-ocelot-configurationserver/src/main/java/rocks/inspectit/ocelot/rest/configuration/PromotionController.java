package rocks.inspectit.ocelot.rest.configuration;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
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
    private FileManager fileManager;

    @ApiOperation(value = "Fetch promotion files", notes = "Fetches all configuration files which are ready for promotion.")
    @GetMapping(value = "configuration/promotions")
    public WorkspaceDiff getPromotions(@ApiParam("Specifies whether the old and new content of each files should also be returned.") @RequestParam(defaultValue = "false", name = "include-content") boolean includeContent) throws IOException, GitAPIException {
        return fileManager.getWorkspaceDiff(includeContent);
    }

    @Secured(UserRoleConfiguration.COMMIT_ACCESS_ROLE)
    @ApiOperation(value = "Promote configurations", notes = "Promotes the specified configuration files.")
    @PostMapping(value = "configuration/promote")
    public ResponseEntity promoteConfiguration(@ApiParam("The definition that contains the information about which files to promote.") @RequestBody ConfigurationPromotion promotion) throws GitAPIException {
        try {
            fileManager.promoteConfiguration(promotion);
            return ResponseEntity.ok().build();
        } catch (ConcurrentModificationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }
}
