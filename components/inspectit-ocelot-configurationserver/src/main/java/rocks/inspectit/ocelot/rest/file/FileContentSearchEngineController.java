package rocks.inspectit.ocelot.rest.file;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import rocks.inspectit.ocelot.rest.AbstractBaseController;
import rocks.inspectit.ocelot.search.FileContentSearchEngine;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;


@RestController
public class FileContentSearchEngineController extends AbstractBaseController {

    @Autowired
    private FileContentSearchEngine fileContentSearchEngine;

    @ApiOperation(value = "Search the given query in all present files.", notes = "Search the given query in all present files.")
    @ApiImplicitParam(name = "SearchString", type = "string", value = "The search string that should be searched in the files.")
    @GetMapping(value = "search")
    public Map getFiles(HttpServletRequest request) {
        String path = request.getParameter("query");
        return fileContentSearchEngine.searchInFiles(path);
    }
}
