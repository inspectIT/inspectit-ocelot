package rocks.inspectit.ocelot.search;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.autocomplete.util.ConfigurationFilesCache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

@Component
public class FileContentSearchEngine {

    @Autowired
    private ConfigurationFilesCache configurationFilesCache;

    /**
     * Searches all files in the working directory of the server for the given query. If limit is set to -1, all
     * occurrences of the given query are returned.
     * Returns a List containing SearchResult objects. Each of these objects resembles one occurrence of the given query
     * and holds information about the file, the start line, the end line, the start column and the end column the query
     * was found in.
     *
     * <p>
     * If the given query is an empty String, an empty Map is returned.
     *
     * @param query The String that is searched for.
     * @param limit The maximum amount of entries that should be searched. Set to -1 to get all entries returned.
     * @return A HashMap containing the file paths and lists of line numbers where the query was found in.
     */
    public List<SearchResult> searchInFiles(String query, int limit) {
        if (!query.isEmpty()) {
            return searchForQuery(query, configurationFilesCache.getFiles(), limit);
        }
        return Collections.emptyList();
    }

    /**
     * Takes a HashMap in which each key resembles a path to a file and each value resembles the files contents as well
     * as a search query and a limit. ALl files contents are searched for this query. Returns a List containing
     * SearchResult objects.
     * Each of these objects resembles one occurrence of the given query and holds information about the file, the start
     * line, the end line, the start column and the end column the query was found in.
     * The maximum amount of SearchResult is defined by the limit parameter. If this parameter is set to -1, all found
     * results are returned.
     *
     * @param query The String that should be searched.
     * @param files A HashMap resembling the files the string should be searched in.
     * @param limit The maximum amount of entries that should be searched. Set to -1 to get all entries returned.
     * @return A HashMap consisting of the file paths as key and a list of line numbers as integers in which the query
     * was found in the respective file.
     */
    private List<SearchResult> searchForQuery(String query, HashMap files, int limit) {
        List<SearchResult> filesMatched = new ArrayList<>();
        for (Object filename : files.keySet()) {
            if (filesMatched.size() == limit) {
                return filesMatched;
            }
            filesMatched.addAll(
                    findMatchingSubStringsInContent(
                            (String) files.get(filename),
                            query,
                            (String) filename,
                            limit - filesMatched.size())
            );
        }
        return filesMatched;
    }

    /**
     * Takes a String resembling a files content, a query that should be searched in the file, a String that resembles
     * the name of the file the content is from and a limit. Returns a list of SearchResult instances which each
     * contains the file name the match was found in, the start line and position of a found query match as well as the
     * end line and position of a found query match.
     * The maximum amount of SearchResult is defined by the limit parameter. If this parameter is set to -1, all found
     * results are returned.
     *
     * @param content  The content that should be searched through.
     * @param query    The query that should be searched in the content.
     * @param fileName The name of the file the currently searched content is from.
     * @param limit    The maximum amount of entries that should be searched. Set to -1 to get all entries returned.
     * @return A list of SearchResult instances which each contains the name of the file the match was found in as well as the
     * start line and column of a found query match and the end line and column of a found query match.
     */
    private List<SearchResult> findMatchingSubStringsInContent(String content, String query, String fileName, int limit) {
        List<SearchResult> search = new ArrayList<>();
        int fromIndex = 0;
        while ((fromIndex = content.indexOf(query, fromIndex)) != -1 && limit != 0) {
            String currentSubstring = content.substring(0, fromIndex);
            String currentSubstringWithQuery = (content.substring(0, fromIndex + query.length()));
            search.add(
                    new SearchResult(
                            fileName,
                            getLineNumberOf(currentSubstring),
                            fromIndex - getLineOffset(currentSubstring) + 1,
                            getLineNumberOf(currentSubstringWithQuery),
                            fromIndex - getLineOffset(currentSubstringWithQuery) + query.length()
                    )
            );
            fromIndex++;
            limit--;
        }
        return search;
    }

    /**
     * Returns the maximum line count of a given String.
     * To do so, all occurrences of '\n' are counted and returned.
     *
     * @param content The String the lines of which should be counted.
     * @return The line number of the given String.
     */
    private int getLineNumberOf(String content) {
        int fromIndex = 0, counter = 1;
        while ((fromIndex = content.indexOf("\n", fromIndex)) != -1) {
            fromIndex++;
            counter++;
        }
        return counter;
    }

    /**
     * Returns the index of the last occurrence of '\n' of a given String.
     *
     * @param content The String of which the line offset should be returned.
     * @return The line offset of the given String.
     */
    private int getLineOffset(String content) {
        if (content.lastIndexOf('\n') == -1) {
            return 0;
        }
        return content.lastIndexOf('\n') + 1;
    }
}
