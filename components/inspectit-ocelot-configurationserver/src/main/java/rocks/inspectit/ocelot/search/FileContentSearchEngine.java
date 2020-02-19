package rocks.inspectit.ocelot.search;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.autocomplete.ConfigurationFilesCache;

import java.util.*;

@Component
public class FileContentSearchEngine {

    @Autowired
    private ConfigurationFilesCache configurationFilesCache;

    /**
     * Searches all files in the working directory of the server for the given query.
     * Returns a HashMap consisting of the paths to the files where the query was found in as keys and a list containing
     * <p>
     * If the given query is an empty String, an empty Map is returned.
     *
     * @param query The String that is searched for.
     * @return A HashMap containing the file paths and lists of line numbers where the query was found in.
     */
    public Map searchInFiles(String query) {
        if (!query.isEmpty()) {
            return searchForQuery(query, getFileContents());
        }
        return Collections.emptyMap();
    }

    /**
     * Takes a HashMap in which each key resembles a path to a file and each value resembles the files contents as well
     * as a search query. ALl files contents are searched for this query. Returns a HashMap consisting of the path to
     * the file and a list of MatchedSubstringIndicators which each defines the start line and position of a found
     * query match as well as the end line and position of a found query match.
     *
     * @param query The String that should be searched.
     * @param files A HashMap resembling the files the string should be searched in.
     * @return A HashMap consisting of the file paths as key and a list of line numbers as integers in which the query
     * was found in the respective file.
     */
    private HashMap<String, List<MatchedSubstringIndicator>> searchForQuery(String query, HashMap files) {
        HashMap<String, List<MatchedSubstringIndicator>> filesMatched = new HashMap<>();
        for (Object filename : files.keySet()) {
            List<MatchedSubstringIndicator> foundLines = findMatchingSubStringsInContent((String) files.get(filename), query);
            if (!foundLines.isEmpty()) {
                filesMatched.put((String) filename, foundLines);
            }
        }
        return filesMatched;
    }

    /**
     * Returns the file currently loaded in the ConfigurationFilesCache.
     *
     * @return The currently loaded files as a HashMap. Each key resembles the path to the file, each value its contents.
     */
    private HashMap<String, String> getFileContents() {
        return configurationFilesCache.getFiles();
    }

    /**
     * Takes a String resembling a files content and a query that should be searched in the file. Returns a list of
     * MatchedSubstringIndicators which each defines the start line and position of a found query match as well as the
     * end line and position of a found query match.
     *
     * @param content The content that should be searched through.
     * @param query   The query that should be searched in the content.
     * @return A list of MatchedSubstringIndicators which each defines the start line and position of a found query
     * match as well as the end line and position of a found query match.
     */
    private List<MatchedSubstringIndicator> findMatchingSubStringsInContent(String content, String query) {
        List<MatchedSubstringIndicator> matchedSubstrings = new ArrayList<>();
        int currentLine = 0;
        int currentLineOffSet = 0;
        int matchedStartLine = 0;
        int matchedStartPosition = 0;
        int queryMatchingIndex = 0;
        for (int i = 0; i < content.length(); i++) {
            char currentChar = content.charAt(i);
            if (currentChar == '\n') {
                currentLine++;
                currentLineOffSet = i;
            }
            if (currentChar == query.charAt(queryMatchingIndex)) {

                if (queryMatchingIndex == 0) {
                    matchedStartLine = currentLine;
                    matchedStartPosition = i - currentLineOffSet;
                }

                if (queryMatchingIndex == query.length() - 1) {
                    matchedSubstrings.add(getMatchedSubstringInstance(matchedStartLine,
                            matchedStartPosition,
                            currentLine,
                            i - currentLineOffSet
                    ));
                    queryMatchingIndex = 0;
                } else {
                    queryMatchingIndex++;
                }
            } else {
                queryMatchingIndex = 0;
            }

        }
        return matchedSubstrings;
    }

    /**
     * Creates a MatchedSubstringIndicator instance and returns it.
     *
     * @param matchedStartLine     The line number in which the matched substring starts.
     * @param matchedStartPosition The position in the given line at which the matched substring starts.
     * @param matchedEndLine       The line number in which the matched substring ends.
     * @param matchedEnPosition    The position in the given line at which the matched substring ends.
     * @return An instance of MatchedSubstringIndicator with all attributes set according to the parameters with which
     * this function was called.
     */
    private MatchedSubstringIndicator getMatchedSubstringInstance(int matchedStartLine,
                                                                  int matchedStartPosition,
                                                                  int matchedEndLine,
                                                                  int matchedEnPosition) {
        MatchedSubstringIndicator subString = new MatchedSubstringIndicator();
        subString.setStart(matchedStartLine, matchedStartPosition);
        subString.setEnd(matchedEndLine, matchedEnPosition);
        return subString;
    }
}
