package rocks.inspectit.ocelot.autocomplete;

import lombok.Data;

@Data
public class AutoCompleteRequest {
    /**
     * The path within the configuration for which suggestions for the next literal shall be provided, e.g. "inspectit.instrumentation
     */
    private String path;
}
