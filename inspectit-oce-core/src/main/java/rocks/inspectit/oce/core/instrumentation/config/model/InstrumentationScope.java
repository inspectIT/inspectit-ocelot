package rocks.inspectit.oce.core.instrumentation.config.model;

import lombok.Value;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

@Value
public class InstrumentationScope {

    private ElementMatcher.Junction<TypeDescription> typeMatcher;

    private ElementMatcher.Junction<MethodDescription> methodMatcher;

}
