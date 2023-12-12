package rocks.inspectit.ocelot.rest.file;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import rocks.inspectit.ocelot.file.FileData;
import rocks.inspectit.ocelot.rest.util.RequestUtil;
import rocks.inspectit.ocelot.search.FileContentSearchEngine;
import rocks.inspectit.ocelot.search.SearchResult;
import rocks.inspectit.ocelot.security.config.UserRoleConfiguration;

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
    @Operation(summary = "Write a file", description = "Creates or overwrites a file with the provided text content")
    @Parameter(name = "Path", schema = @Schema(type = "string"), description = "The part of the url after /files/ defines the path to the file to write.")
    @PutMapping(value = "files/**")
    public void writeFile(HttpServletRequest request,
        @Parameter(description = "If true, the request body is not parsed as json and is instead written directly to the result file.")
        @RequestParam(defaultValue = "false")
        boolean raw,
        @Parameter(description = "The content to write, either raw or a json",
            content = {
                @Content(mediaType = "text/plain", examples = @ExampleObject("This is the file content")),
                @Content(mediaType = "application/json", examples = @ExampleObject("{ 'content' : 'This is the file content' }"))
            }
        )
        @RequestBody(required = false)
        String content
    ) throws IOException {
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

    @Operation(summary = "Read a file", description = "Returns the contents of the given file.")
    @Parameter(name = "Path", schema = @Schema(type = "string"), description = "The part of the url after /files/ defines the path to the file to read.")
    @ApiResponse(responseCode = "200", description = "Ok", content = {
        @Content(mediaType = "application/json", examples = @ExampleObject("{ 'content' : 'This is the file content' }")),
        @Content(mediaType = "text/plain", examples = @ExampleObject("This is the file content"))
    })
    @GetMapping(value = "files/**")
    public Object readFile(HttpServletRequest request, @Parameter(description = "If true, the response body is not formatted as json and is instead the plain text" + " content of the file.") @RequestParam(defaultValue = "false") boolean raw, @RequestParam(value = "version", required = false) String commitId) {
        String path = RequestUtil.getRequestSubPath(request);

        Optional<String> contentOptional;
        if (commitId == null) {
            contentOptional = fileManager.getWorkingDirectory().readConfigurationFile(path);
        } else if (commitId.equals("live")) {
            contentOptional = fileManager.getLiveRevision().readConfigurationFile(path);
        } else {
            contentOptional = fileManager.getCommitWithId(commitId).readConfigurationFile(path);
        }

        if (!contentOptional.isPresent()) {
            return null;
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
    @Operation(summary = "Delete a file", description = "Deletes the given file")
    @Parameter(name = "Path", schema = @Schema(type = "string"), description = "The part of the url after /files/ defines the path to the file to delete.")
    @DeleteMapping(value = "files/**")
    public void deleteFile(HttpServletRequest request) throws IOException {
        String path = RequestUtil.getRequestSubPath(request);

        fileManager.getWorkingDirectory().deleteConfiguration(path);
    }

    @Operation(summary = "Search the given query in all present files.", description = "Searches the given query in all present files. " + "Searches for as many matches as defined by the limit parameter. If the the limit is set " + "to -1, the query is searched for all occurrences in all files. All found matches are " + "returned in a list of SearchResult instances. Each of these instances contains the " + "following variables:" + "<p>" + "<b>file:</b> a String resembling the name of the file the match was found in." + "<p>" + "<b>firstLine:</b> the first line of the found match. Only retrieved if retrieveFirstLine is true " + "<p>" + "<b>startLine:</b> the number of the line in this file where the found match starts as " + "integer." + "<p>" + "<b>endLine:</b> the number of the line in this file where the found match ends as integer." + "<p>" + "<b>startColumn:</b> the number of the column where the found found match starts as " + "integer." + "<p>" + "<b>endColumn:</b> the number of the column where the found match ends as integer.")
    @Parameters({@Parameter(name = "query", description = "The query string that should be searched in the files."), @Parameter(name = "limit", description = "The limit for the returned values. Use '-1' for no limit."), @Parameter(name = "include-first-line", description = "If true, the first line of each match is added to the search results.")})
    @GetMapping(value = {"search", "search/"})
    public List<SearchResult> searchForContent(@RequestParam String query, @RequestParam(defaultValue = "100") int limit, @RequestParam(name = "include-first-line", defaultValue = "false") boolean includeFirstLine) {
        return fileContentSearchEngine.search(query, limit, includeFirstLine);
    }
}
