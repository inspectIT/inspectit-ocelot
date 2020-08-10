package rocks.inspectit.ocelot.search;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.file.FileInfo;
import rocks.inspectit.ocelot.file.FileManager;
import rocks.inspectit.ocelot.file.accessor.git.RevisionAccess;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class FileContentSearchEngine {

    @Autowired
    private FileManager fileManager;

    /**
     * Searches in all files in the current Workspace Revision of the server for the given query. The minimum amount of
     * returned entries is the amount that could be found, the maximum amount of returned entries is defined by the
     * limit parameter.
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
     * The maximum amount of SearchResults is defined by the limit parameter.
     *
     * @param query          The String that should be searched.
     * @param revisionAccess The {@link RevisionAccess} instance of the current Workspace.
     * @param limit          The maximum amount of entries that should be searched.
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
                    .get(), query, file.getName(), limit + filesMatched.size()));

        }
        return filesMatched;
    }

    /**
     * Takes a String resembling a files content, a String that resembles a query that should be searched in the files
     * content, a String that resembles the name of the file the content is from and an integer resembling the search-limit.
     * Returns a list of {@link SearchResult} instances of which each resembles a found query match.
     * The maximum amount of SearchResult instances is defined by the limit parameter.
     *
     * @param content  The content that should be searched through.
     * @param query    The query that should be searched in the content.
     * @param fileName The name of the file the currently searched content is from.
     * @param limit    The maximum amount of entries that should be searched.
     *
     * @return A list of {@link SearchResult} instances. Each instance resembles one match.
     */
    private List<SearchResult> findMatchingSubStringsInContent(String content, String query, String fileName, int limit) {
        List<SearchResult> result = new ArrayList<>();
        List<Integer> startIndices = new ArrayList<>();
        List<Integer> endIndices = new ArrayList<>();

        collectMatchingIndices(content, query, limit, startIndices, endIndices);

        Map<Integer, List<Integer>> matchesStart = resolveLineNumbersAndColumns(content, startIndices);
        Map<Integer, List<Integer>> matchesEnd = resolveLineNumbersAndColumns(content, endIndices);
        Integer[] startKeySet = matchesStart.keySet().toArray(new Integer[0]);
        Integer[] endKeySet = matchesEnd.keySet().toArray(new Integer[0]);

        for (int i = 0; i < matchesStart.size(); i++) {
            for (int j = 0; j < matchesStart.get(startKeySet[i]).size(); j++) {
                int startLine = startKeySet[i];
                int startColumn = matchesStart.get(startLine).get(j);
                int endLine = endKeySet[i];
                int endColumn = matchesEnd.get(endLine).get(j);
                result.add(new SearchResult(fileName, startLine, startColumn, endLine, endColumn));
            }
        }
        return result;
    }

    /**
     * Takes a String resembling a files content, a String resembling a query to search and an integer resembling
     * a limit. All indices where a match was found are added to the list given in the startIndices parameter. All
     * corresponding end indices are added to the list given in the endIndices parameter.
     * The maximum amount of searched starting indices can be defined by the limit-parameter.
     *
     * @param content      The content that should be searched through.
     * @param query        The query that should be searched in the content.
     * @param limit        The maximum amount of entries that should be searched.
     * @param startIndices The List in which all start indices of the matches should be saved in.
     * @param endIndices   The List in which all end indices of the matches should be saved in.
     */
    private void collectMatchingIndices(String content, String query, int limit, List<Integer> startIndices, List<Integer> endIndices) {
        Pattern searchPattern = Pattern.compile(Pattern.quote(query));
        Matcher matcher = searchPattern.matcher(content);
        while (matcher.find() && limit > startIndices.size()) {
            startIndices.add(matcher.start());
            endIndices.add(matcher.end());
        }
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
        Pattern searchPattern = Pattern.compile(Pattern.quote("\n"));
        Matcher matcher = searchPattern.matcher(content);
        int lineNumber = 0;
        for (Integer index : matchingIndices) {
            matcher.region(0, index);
            while (matcher.find()) {
                lineNumber++;
            }
            int lineOffset = content.lastIndexOf("\n", index) + 1;
            lineNumbers.computeIfAbsent(lineNumber, k -> new ArrayList<>());
            lineNumbers.get(lineNumber).add(index - lineOffset);
        }
        return lineNumbers;
    }
}
