package rocks.inspectit.ocelot.search;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * This class resembles a query matched by the FileContentSearchEngine. The file variable contains the file name the
 * query was found in. The start variables provide information on in which line and on which column in this line a
 * searched substring was found. The end variable does alike for the end of the query.
 */
@AllArgsConstructor
@Data
public class SearchResult {

    /**
     * The name of the file the match was found in.
     */
    private String file;

    /**
     * The start line of the match.
     */
    private int startLine;

    /**
     * The start column of the match in the starting line.
     */
    private int startColumn;

    /**
     * The end line of the match.
     */
    private int endLine;

    /**
     * The end column of the match in the ending line.
     */
    private int endColumn;

}
