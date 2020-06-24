package rocks.inspectit.ocelot.rest.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rocks.inspectit.ocelot.file.FileManager;
import rocks.inspectit.ocelot.file.versioning.model.WorkspaceDiff;
import rocks.inspectit.ocelot.rest.AbstractBaseController;

@RestController
@Slf4j
public class PromotionController extends AbstractBaseController {

    @Autowired
    private FileManager fileManager;

    @GetMapping(value = "configuration/promotions")
    public WorkspaceDiff getPromotions(@RequestParam(defaultValue = "false", name = "include-content") boolean includeContent) {
        return fileManager.getWorkspaceDiff(includeContent);
    }

}
