package rocks.inspectit.oce.eum.server.configuration.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class PatternAndReplacement {

    String pattern;

    String replacement;
}
