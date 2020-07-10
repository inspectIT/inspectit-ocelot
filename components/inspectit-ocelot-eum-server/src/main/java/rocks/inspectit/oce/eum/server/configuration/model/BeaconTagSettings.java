package rocks.inspectit.oce.eum.server.configuration.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

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
     * Decides what to do in case no match for the given Regex is found.
     * If this is true, the input value will be used unchanged as value for the output in case no RegexMatch is found.
     * <p>
     * If this value is false(default), then no output will be generated if no match is found.
     */
    @Builder.Default
    private boolean keepNoMatch = false;

    /**
     * The name of the input field to use, e.g. "u" for the request URL.
     */
    @NotBlank
    private String input;

    /**
     * The regex pattern to use, capture groups are supported!
     * <p>
     * If this regex is null or empty, the input value will be copied unchanged.
     */
    private String regex;

    /**
     * The replacement to apply on each match, $1,§2,$2 can be used to refer to capture groups.
     */
    @NotNull
    @Builder.Default
    private String replacement = "";

    /**
     * Specify whether the input field should be considered as an empty string if it does not exists.
     */
    @Builder.Default
    private boolean nullAsEmpty = false;
}
