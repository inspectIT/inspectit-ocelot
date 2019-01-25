package rocks.inspectit.oce.core.config.model.instrumentation;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import java.util.Map;

@Data
@NoArgsConstructor
public class InstrumentationProfile {

    private String name;

    private boolean enabled;

    private Integer priority;

    @Valid
    private Map<String, Boolean> sensors;

}
