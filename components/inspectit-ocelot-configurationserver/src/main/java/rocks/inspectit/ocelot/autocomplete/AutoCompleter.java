package rocks.inspectit.ocelot.autocomplete;

import java.util.List;

public interface AutoCompleter {
    /**
     * Returns a list of suggestions for a given path. Suggestions are String literals which could be added to the given
     * path.
     *
     * @param path A given path as List. Each String should act as a literal of the path.
     * @return A list of strings. Each string resembles one literal that could be appended to the given path.
     */
    List<String> getSuggestions(List<String> path);
}
