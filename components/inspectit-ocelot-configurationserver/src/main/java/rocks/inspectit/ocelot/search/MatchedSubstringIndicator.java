package rocks.inspectit.ocelot.search;

import lombok.Data;

/**
 * This class resembles a substring matched by the FileContentSearchEngine. The start variable provides information on
 * in which line and on which position in this line a searched substring was found. The end variable does so for the end
 * of the substring.
 */
@Data
public class MatchedSubstringIndicator {
    private MatchedSubstringValuePair start, end;

    /**
     * Sets the MatchedSubstringValuePair which resembles the start of a matched substrings with the given parameters.
     *
     * @param line     the line in which the matched substring starts.
     * @param position the position in the given line in which matched substring starts.
     */
    public void setStart(int line, int position) {
        start = new MatchedSubstringValuePair(line, position);
    }

    /**
     * Sets the MatchedSubstringValuePair which resembles the end of a matched substrings with the given parameters.
     *
     * @param line     the line in which the matched substring ends.
     * @param position the position in the given line in which matched substring ends.
     */
    public void setEnd(int line, int position) {
        end = new MatchedSubstringValuePair(line, position);
    }

    /**
     * This class resembles a value pair to determine the start or the end of a matched substring. The value pair
     * consists of a line number and a position in this line number, both as int values.
     */
    @Data
    private class MatchedSubstringValuePair {
        private int line, position;

        private MatchedSubstringValuePair(int line, int position) {
            setLine(line);
            setPosition(position);
        }
    }

}
