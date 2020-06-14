package rocks.inspectit.ocelot.search;

import lombok.Data;

/**
 * This class resembles a query matched by the FileContentSearchEngine. The file variable contains the file name the
 * query was found in. The start variables provide information on in which line and on which column in this line a
 * searched substring was found. The end variable does alike for the end of the query.
 */
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
     * The start column of the match.
     */
    private int startColumn;

    /**
     * The end line of the match.
     */
    private int endLine;

    /**
     * The end column of the match.
     */
    private int endColumn;

    public SearchResult(String file, int startLine, int startColumn, int endLine, int endColumn) {
        this.file = file;
        this.startLine = startLine;
        this.startColumn = startColumn;
        this.endLine = endLine;
        this.endColumn = endColumn;
    }
}
