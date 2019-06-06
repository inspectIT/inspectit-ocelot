package rocks.inspectit.ocelot.rest.file;

import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Controller for managing the configurations.
 */
@RestController
public class AssetController extends FileBaseController {

    @PutMapping(value = "assets/**")
    public void writeAsset(HttpServletRequest request, @RequestBody(required = false) String content) throws IOException {
        String path = getRequestSubPath(request);
        files.createOrReplaceFile(path, content == null ? "" : content);
    }

    @GetMapping(value = "assets/**")
    public String readAsset(HttpServletRequest request) throws IOException {
        String path = getRequestSubPath(request);
        return files.readFile(path);
    }

    @DeleteMapping(value = "assets/**")
    public void deleteAsset(HttpServletRequest request) throws IOException {
        String path = getRequestSubPath(request);
        files.deleteFile(path);
    }
}
