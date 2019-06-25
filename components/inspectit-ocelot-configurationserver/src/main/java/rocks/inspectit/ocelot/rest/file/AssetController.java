package rocks.inspectit.ocelot.rest.file;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import rocks.inspectit.ocelot.file.FileData;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Controller for managing the configurations.
 */
@RestController
public class AssetController extends FileBaseController {

    @Autowired
    private ObjectMapper objectMapper;

    @PutMapping(value = "assets/**")
    public void writeAsset(HttpServletRequest request, @RequestParam(defaultValue = "false") boolean raw, @RequestBody(required = false) String content) throws IOException {
        String path = getRequestSubPath(request);
        if (raw || content == null) {
            files.createOrReplaceFile(path, content == null ? "" : content);
        } else {
            FileData data = objectMapper.readValue(content, FileData.class);
            files.createOrReplaceFile(path, data.getContent());
        }
    }

    @GetMapping(value = "assets/**")
    public Object readAsset(HttpServletRequest request, @RequestParam(defaultValue = "false") boolean raw) throws IOException {
        String path = getRequestSubPath(request);
        String content = files.readFile(path);
        if (raw) {
            return content;
        } else {
            return FileData.builder().content(content).build();
        }
    }

    @DeleteMapping(value = "assets/**")
    public void deleteAsset(HttpServletRequest request) throws IOException {
        String path = getRequestSubPath(request);
        files.deleteFile(path);
    }
}
