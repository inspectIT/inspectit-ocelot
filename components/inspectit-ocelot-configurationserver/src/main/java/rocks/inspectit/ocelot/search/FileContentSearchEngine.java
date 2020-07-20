package rocks.inspectit.ocelot.search;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.file.FileInfo;
import rocks.inspectit.ocelot.file.FileManager;
import rocks.inspectit.ocelot.file.accessor.git.RevisionAccess;

import java.util.*;

@Component
public class FileContentSearchEngine {

    @Autowired
    private FileManager fileManager;

    /**
     * Searches in all files in the current Workspace Revision of the server for the given query. If limit is set to -1,
     * all matches of the given query are returned, otherwise only the given amount of matches is searched for.
     * Returns a List containing {@link SearchResult} instances. Each of these instances resembles one occurrence of the
     * given query.
     * <p>
     * If the given query is an empty String, an empty Map is returned.
     *
     * @param query The String that is searched for.
     * @param limit The maximum amount of entries that should be searched. Set to -1 to get all entries returned.
     *
     * @return A List containing {@link SearchResult} instances for each found match.
     */
    public List<SearchResult> searchInFiles(String query, int limit) {
        if (!query.isEmpty()) {
            return searchForQuery(query, fileManager.getWorkspaceRevision(), limit);
        }
        return Collections.emptyList();
    }

    /**
     * Takes a String resembling a search-query, a {@link RevisionAccess} instance resembling the current workspace-revision,
     * aswell as a search-limit. ALl files present in the workspace-revision are searched for the given query.
     * Returns a List containing {@link SearchResult} instances. Each of these instances resembles one occurrence of the
     * given query.
     * The maximum amount of SearchResults is defined by the limit parameter. If this parameter is set to -1 the
     * algorithm retrieves in all present files all occurences of the query.
     *
     * @param query          The String that should be searched.
     * @param revisionAccess The {@link RevisionAccess} instance of the current Workspace.
     * @param limit          The maximum amount of entries that should be searched. Set to -1 to get all entries returned.
     *
     * @return A List containing {@link SearchResult} for each found match.
     */
    private List<SearchResult> searchForQuery(String query, RevisionAccess revisionAccess, int limit) {
        List<SearchResult> filesMatched = new ArrayList<>();
        for (FileInfo file : revisionAccess.listConfigurationFiles("")) {
            if (filesMatched.size() == limit) {
                return filesMatched;
            }
            filesMatched.addAll(findMatchingSubStringsInContent(revisionAccess.readConfigurationFile(file.getName())
                    .get(), query, file.getName(), limit - filesMatched.size()));

        }
        return filesMatched;
    }

    /**
     * Takes a String resembling a files content, a String that resembles a query that should be searched in the files
     * content, a String that resembles the name of the file the content is from and an integer resembling the search-limit.
     * Returns a list of {@link SearchResult} instances of which each resembles a found query match.
     * The maximum amount of SearchResult instances is defined by the limit parameter. If this parameter is set to -1
     * the algorithm retrieves in all present files all occurences of the query.
     *
     * @param content  The content that should be searched through.
     * @param query    The query that should be searched in the content.
     * @param fileName The name of the file the currently searched content is from.
     * @param limit    The maximum amount of entries that should be searched. Set to -1 to get all entries returned.
     *
     * @return A list of {@link SearchResult} instances. Each instance resembles one match.
     */
    private List<SearchResult> findMatchingSubStringsInContent(String content, String query, String fileName, int limit) {
        List<SearchResult> result = new ArrayList<>();
        Map<Integer, List<Integer>> matches = resolveLineNumbersAndColumns(content, collectMatchingIndices(content, query, limit));
        for (int lineNumber : matches.keySet()) {
            for (int index : matches.get(lineNumber)) {
                result.add(new SearchResult(fileName, lineNumber, index, lineNumber + countLineBreaks(query), getEndColumn(query, index)));
            }
        }
        return result;
    }

    /**
     * Takes a String resembling a files content, a String resembling a query to search and an integer resembling a limit.
     * Returns a List of Integers. Each Integer resembles the starting index where the query was found in the content.
     * The maximum amount of searched starting indices can be defined by the limit-parameter.
     *
     * @param content The content that should be searched through.
     * @param query   The query that should be searched in the content.
     * @param limit   The maximum amount of entries that should be searched. Set to -1 to get all entries returned.
     *
     * @return An ascending ordered List of Integers, each of which resemble an index in the content parameter where the query parameter
     * occurs.
     */
    private List<Integer> collectMatchingIndices(String content, String query, int limit) {
        List<Integer> matchingIndices = new ArrayList<>();
        int matchIndex = 0;
        while ((matchIndex = content.indexOf(query, matchIndex)) != -1 && limit != 0) {
            matchingIndices.add(matchIndex);
            matchIndex++;
            limit--;
        }
        return matchingIndices;
    }

    /**
     * Takes a String resembling a files content and an ascending ordered List of integers resembling indices in the
     * content parameter. For each given index, the line number as well as the column number is determined.
     *
     * @param content         The content of the current file.
     * @param matchingIndices An ascending ordered List of integers resembling indices in the content parameter.
     *
     * @return A map consisting of the line numbers as keys and a list of integers as the values. Each integer in the list
     * resembles the start column of a match of the searched query.
     */
    private Map<Integer, List<Integer>> resolveLineNumbersAndColumns(String content, List<Integer> matchingIndices) {
        Map<Integer, List<Integer>> lineNumbers = new TreeMap<>();
        int currentIndex = 0, lineNumber = 0, lineOffset = 0;
        for (Integer index : matchingIndices) {
            while ((currentIndex = content.indexOf("\n", currentIndex)) != -1 && currentIndex < index) {
                currentIndex++;
                lineNumber++;
            }
            lineNumbers.computeIfAbsent(lineNumber, k -> new ArrayList<>());
            lineNumbers.get(lineNumber).add(index - lineOffset);
        }
        return lineNumbers;
    }

    /**
     * Takes a String resembling a search query. Counts the amount of line breaks present in that search query.
     *
     * @param query The query the amount of line breaks should be counted of.
     *
     * @return The amount of line breaks in the query.
     */
    private int countLineBreaks(String query) {
        int fromIndex = 0, lineBreaks = 0;
        while ((fromIndex = query.indexOf("\n", fromIndex)) != -1) {
            fromIndex++;
            lineBreaks++;
        }
        return lineBreaks;
    }

    /**
     * Takes a String resembling a search query and an int resembling the start column of an occurence of that query.
     * If the query does not contain any line breaks, the start column + the length of the query is returned.
     * If the query contains line breaks, the length of the end column of the query in the last line is returned.
     *
     * @param query       The query the end column should be returned of.
     * @param startColumn The start column of an occurence of that query.
     *
     * @return The end column of that query.
     */
    private int getEndColumn(String query, int startColumn) {
        if (query.contains("\n")) {
            String[] s = query.split("\n");
            return s[s.length - 1].length();
        }
        return startColumn + query.length();
    }
}
