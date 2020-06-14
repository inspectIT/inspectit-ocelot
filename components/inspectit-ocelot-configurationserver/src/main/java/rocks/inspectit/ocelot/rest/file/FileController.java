package rocks.inspectit.ocelot.rest.file;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import rocks.inspectit.ocelot.file.FileData;
import rocks.inspectit.ocelot.rest.util.RequestUtil;
import rocks.inspectit.ocelot.search.FileContentSearchEngine;
import rocks.inspectit.ocelot.search.SearchResult;
import rocks.inspectit.ocelot.security.config.UserRoleConfiguration;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Controller for managing the configurations.
 */
@RestController
public class FileController extends FileBaseController {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FileContentSearchEngine fileContentSearchEngine;

    @Secured(UserRoleConfiguration.WRITE_ACCESS_ROLE)
    @ApiOperation(value = "Write a file", notes = "Creates or overwrites a file with the provided text content")
    @ApiImplicitParam(name = "Path", type = "string", value = "The part of the url after /files/ defines the path to the file to write.")
    @PutMapping(value = "files/**")
    public void writeFile(HttpServletRequest request,
                          @ApiParam("If true, the request body is not parsed as json and is instead written directly to the result file.") @RequestParam(defaultValue = "false") boolean raw,
                          @ApiParam(value = "The content to write, either raw or a json",
                                  examples = @Example(value = {
                                          @ExampleProperty(mediaType = "application/json", value = "{ 'content' : 'This is the file content' }"),
                                          @ExampleProperty(mediaType = "text/plain", value = "This is the file content")
                                  })
                          )

                          @RequestBody(required = false) String content) throws IOException {
        String path = RequestUtil.getRequestSubPath(request);

        String fileContent;
        if (raw || content == null) {
            fileContent = content == null ? "" : content;
        } else {
            FileData data = objectMapper.readValue(content, FileData.class);
            fileContent = data.getContent();
        }

        fileManager.getWorkingDirectory().writeConfigurationFile(path, fileContent);
    }

    @ApiOperation(value = "Read a file", notes = "Returns the contents of the given file.")
    @ApiImplicitParam(name = "Path", type = "string", value = "The part of the url after /files/ defines the path to the file to read.")
    @ApiResponse(code = 200,
            message = "Ok",
            examples = @Example(value = {
                    @ExampleProperty(mediaType = "application/json", value = "{ 'content' : 'This is the file content' }"),
                    @ExampleProperty(mediaType = "text/plain", value = "This is the file content")
            }))
    @GetMapping(value = "files/**")
    public Object readFile(HttpServletRequest request,
                           @ApiParam("If true, the response body is not formatted as json and is instead the plain text content of the file.")
                           @RequestParam(defaultValue = "false") boolean raw) {
        String path = RequestUtil.getRequestSubPath(request);
        Optional<String> contentOptional = fileManager.getWorkingDirectory().readConfigurationFile(path);

        if (!contentOptional.isPresent()) {
            return ResponseEntity.notFound();
        }

        return contentOptional.map(content -> {
            if (raw) {
                return content;
            } else {
                return FileData.builder().content(content).build();
            }
        });
    }

    @Secured(UserRoleConfiguration.WRITE_ACCESS_ROLE)
    @ApiOperation(value = "Delete a file", notes = "Deletes the given file")
    @ApiImplicitParam(name = "Path", type = "string", value = "The part of the url after /files/ defines the path to the file to delete.")
    @DeleteMapping(value = "files/**")
    public void deleteFile(HttpServletRequest request) throws IOException {
        String path = RequestUtil.getRequestSubPath(request);

        fileManager.getWorkingDirectory().deleteConfiguration(path);
    }

    @ApiOperation(value = "Search the given query in all present files.", notes = "Search the given query in all present files.")
    @ApiImplicitParam(name = "query", type = "string", value = "The query string that should be searched in the files.")
    @GetMapping(value = "search")
    public List<SearchResult> searchQuery(@RequestParam String query, @RequestParam(defaultValue = "-1") int limit) {
        return fileContentSearchEngine.searchInFiles(query, limit);
    }
}
