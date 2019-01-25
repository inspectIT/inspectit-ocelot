package rocks.inspectit.oce.core.config.model.instrumentation.sensor;

import lombok.Data;
import lombok.NoArgsConstructor;
import net.bytebuddy.matcher.StringMatcher;

import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
public class SensorTargetType {

    private Boolean isInterface;

    private Boolean isAbstract;

    @NotNull
    private String namePattern;

    @NotNull
    private StringMatcher.Mode matcherMode;
}
