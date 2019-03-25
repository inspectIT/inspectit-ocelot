package rocks.inspectit.ocelot.instrumentation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface MyMethodAnnotation {

    String value();
}
