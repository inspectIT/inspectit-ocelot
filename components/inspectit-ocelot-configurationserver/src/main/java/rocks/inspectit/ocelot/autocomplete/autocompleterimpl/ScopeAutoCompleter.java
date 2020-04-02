package rocks.inspectit.ocelot.autocomplete.autocompleterimpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.autocomplete.AutoCompleter;
import rocks.inspectit.ocelot.autocomplete.util.ConfigurationQueryHelper;
import rocks.inspectit.ocelot.config.validation.PropertyPathHelper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This AutoCompleter retrieves all scopes which can be found in the present yaml-files. It is either triggered by
 * the path "inspectit.instrumentation.scopes" or "inspectit.instrumentation.rules.*.scopes"
 */
@Slf4j
@Component
public class ScopeAutoCompleter implements AutoCompleter {

    private final static List<List<String>> SCOPE_PATHS = Arrays.asList(
            Arrays.asList("inspectit", "instrumentation", "scopes"),
            Arrays.asList("inspectit", "instrumentation", "rules", "*", "scopes")
    );

    @Autowired
    private ConfigurationQueryHelper configurationQueryHelper;

    @Override
    public List<String> getSuggestions(List<String> path) {
        if (SCOPE_PATHS.stream().anyMatch(scopePath -> PropertyPathHelper.hasPathPrefix(scopePath, path))) {
            return SCOPE_PATHS.stream().
                    filter(scopePath -> !PropertyPathHelper.hasPathPrefix(SCOPE_PATHS.get(0), path) || !PropertyPathHelper.hasPathPrefix(scopePath, SCOPE_PATHS.get(1))).
                    flatMap(scopePath -> configurationQueryHelper.getKeysForPath(scopePath).stream())
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
