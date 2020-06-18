package rocks.inspectit.ocelot.rest.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rocks.inspectit.ocelot.file.FileManager;
import rocks.inspectit.ocelot.file.versioning.ConfigurationPromotion;
import rocks.inspectit.ocelot.file.versioning.model.WorkspaceDiff;
import rocks.inspectit.ocelot.rest.AbstractBaseController;

import java.util.ConcurrentModificationException;

@RestController
@Slf4j
public class PromotionController extends AbstractBaseController {

    @Autowired
    private FileManager fileManager;

    @GetMapping(value = "configuration/promotions")
    public WorkspaceDiff getPromotions(@RequestParam(defaultValue = "false", name = "include-content") boolean includeContent) {
        return fileManager.getWorkspaceDiff(includeContent);
    }

    @PostMapping(value = "configuration/promote")
    public ResponseEntity promoteConfiguration(@RequestBody ConfigurationPromotion promotion) {
        try {
            fileManager.promoteConfiguration(promotion);
            return ResponseEntity.ok().build();
        } catch (ConcurrentModificationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }
}
