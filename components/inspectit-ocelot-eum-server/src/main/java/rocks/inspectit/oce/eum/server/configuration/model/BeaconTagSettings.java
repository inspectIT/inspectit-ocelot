package rocks.inspectit.oce.eum.server.configuration.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
/**
 * Defines how a custom beacon field is derived using a RegEx replaceAll operation from an existing beacon field.
 * If no regex is specified, the provided input field will simply be copied.
 */
public class BeaconTagSettings {

    /**
     * Deprecated in favor of {@link #replacements}.
     * <p>
     * Decides what to do in case no match for the given Regex is found.
     * If this is true, the input value will be used unchanged as value for the output in case no RegexMatch is found.
     * <p>
     * If this value is false(default), then no output will be generated if no match is found.
     */
    @Builder.Default
    @Deprecated
    private boolean keepNoMatch = false;

    /**
     * The name of the input field to use, e.g. "u" for the request URL.
     */
    @NotBlank
    private String input;

    /**
     * Deprecated in favor of {@link #replacements}.
     * The regex pattern to use, capture groups are supported!
     * <p>
     * If this regex is null or empty, the input value will be copied unchanged.
     */
    @Deprecated
    private String regex;

    /**
     * Deprecated in favor of {@link #replacements}.
     * The replacement to apply on each match, $1,§2,$2 can be used to refer to capture groups.
     */
    @NotNull
    @Builder.Default
    @Deprecated
    private String replacement = "";

    /**
     * Specify whether the input field should be considered as an empty string if it does not exists.
     */
    @Builder.Default
    private boolean nullAsEmpty = false;

    /**
     * After the tag value has been extracted, these replacements will be applied in order to the tag value.
     */
    @Builder.Default
    private List<PatternAndReplacement> replacements = Collections.emptyList();

    /**
     * @return All replacements to perform, including the one specified via the deprecated settings.
     */
    public List<PatternAndReplacement> getAllReplacements() {
        List<PatternAndReplacement> result = new ArrayList<>();
        if (regex != null) {
            result.add(PatternAndReplacement.builder()
                    .pattern(regex)
                    .replacement(replacement)
                    .keepNoMatch(keepNoMatch)
                    .build());
        }
        result.addAll(replacements);
        return result;
    }
}
