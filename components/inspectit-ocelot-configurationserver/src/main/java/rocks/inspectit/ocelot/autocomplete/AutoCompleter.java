package rocks.inspectit.ocelot.autocomplete;

import java.util.List;

public interface AutoCompleter {
    List<String> getSuggestions(List<String> camelCasePath);
}
