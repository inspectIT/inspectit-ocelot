package rocks.inspectit.oce.core.config.model.instrumentation.sensor;

import lombok.Data;
import lombok.NoArgsConstructor;
import net.bytebuddy.matcher.StringMatcher;

import javax.validation.constraints.NotNull;
import java.util.Map;

@Data
@NoArgsConstructor
public class SensorTargetMethod {

    private Boolean isConstructor;

    private Boolean isSynchronized;

    private Boolean isPublic;

    private Boolean isProtected;

    private Boolean isPackagePrivate;

    private Boolean isPrivate;

//    private List<Argument> arguments;

    private Map<Integer, String> arguments;

    @NotNull
    private String namePattern;

    @NotNull
    private StringMatcher.Mode matcherMode;

    @Data
    @NoArgsConstructor
    public class Argument {

        private int index;

        private String className;
    }
}
