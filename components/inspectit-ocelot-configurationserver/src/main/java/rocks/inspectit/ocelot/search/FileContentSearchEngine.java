package rocks.inspectit.ocelot.search;

import lombok.Value;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.file.FileInfo;
import rocks.inspectit.ocelot.file.FileManager;
import rocks.inspectit.ocelot.file.accessor.git.RevisionAccess;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Component to search for a specific pattern in the configuration files.
 */
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
    public List<SearchResult> search(String query, int limit) {
        if (query.isEmpty()) {
            return Collections.emptyList();
        }

        return search(query, limit, fileManager.getWorkspaceRevision());
    }

    /**
     * Searches in all configuration files which can be accessed by the given {@link RevisionAccess} for the specified
     * query string. The amount of results can be limited using the limit argument.
     *
     * @param query          the query string to look for
     * @param limit          the maximum amount of results
     * @param revisionAccess the accessor for fetching the files
     *
     * @return a list of {@link SearchResult} representing the matches
     */
    private List<SearchResult> search(String query, int limit, RevisionAccess revisionAccess) {
        Pattern queryPattern = Pattern.compile(Pattern.quote(query));
        List<FileInfo> files = revisionAccess.listConfigurationFiles("");

        AtomicInteger limitCounter = new AtomicInteger(limit);

        List<SearchResult> result = files.stream().map(FileInfo::getName).map(fileName -> {
            Optional<String> content = revisionAccess.readConfigurationFile(fileName);
            return content.map(fileContent -> findQuery(fileName, fileContent, queryPattern, limitCounter));
        }).filter(Optional::isPresent).map(Optional::get).flatMap(Collection::stream).collect(Collectors.toList());

        return result;
    }

    /**
     * Searches in the specified content for the specified query pattern. The passed content represents the content of the
     * file with the specified name.
     *
     * @param fileName     the filename of the current file
     * @param content      the file's content
     * @param queryPattern the pattern to search for
     * @param limitCounter the amount of results to add
     *
     * @return a list of {@link SearchResult} representing the matches
     */
    private List<SearchResult> findQuery(String fileName, String content, Pattern queryPattern, AtomicInteger limitCounter) {
        List<SearchResult> results = new ArrayList<>();

        List<Line> lines = getLines(content);

        ListIterator<Line> listIterator = lines.listIterator();
        Line currentLine = listIterator.next();

        Matcher matcher = queryPattern.matcher(content);
        while (matcher.find() && limitCounter.decrementAndGet() >= 0) {
            int start = matcher.start();
            int end = matcher.end();

            while (start >= currentLine.getEndIndex()) {
                currentLine = listIterator.next();
            }
            int startLine = currentLine.getLineNumber();
            int relativeStart = start - currentLine.getStartIndex();

            while (end > currentLine.getEndIndex()) {
                currentLine = listIterator.next();
            }
            int endLine = currentLine.getLineNumber();
            int relativeEnd = end - currentLine.getStartIndex();

            System.out.println(startLine + "(" + start + "/" + relativeStart + ")\t" + endLine + "(" + end + "/" + relativeEnd + ")");
            SearchResult result = new SearchResult(fileName, startLine, relativeStart, endLine, relativeEnd);
            results.add(result);
        }

        return results;
    }

    /**
     * Extracts a list of {@link Line}s of the given content.
     *
     * @param content the content used as basis
     *
     * @return list of {@link Line}s representing the content
     */
    private List<Line> getLines(String content) {
        if (StringUtils.isEmpty(content)) {
            return Collections.emptyList();
        }

        List<Line> result = new LinkedList<>();
        int lineNumber = 0;
        int startIndex = 0;
        do {
            int nextIndex = content.indexOf("\n", startIndex) + 1;

            // in case there are no further line breaks
            if (nextIndex == 0) {
                nextIndex = content.length();
            }

            Line line = new Line(lineNumber++, startIndex, nextIndex);
            result.add(line);

            startIndex = nextIndex;
        } while (startIndex < content.length());

        return result;
    }

    /**
     * Class for representing a line in a string.
     */
    @Value
    private class Line {

        /**
         * The line number.
         */
        int lineNumber;

        /**
         * The absolute index where the line is starting.
         */
        int startIndex;

        /**
         * The absolute end index where the line is ending.
         */
        int endIndex;
    }
}
