package rocks.inspectit.oce.core.instrumentation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.oce.core.config.InspectitEnvironment;

import java.lang.instrument.Instrumentation;

@Component
public class IgnoredClassesManager {

    @Autowired
    InspectitEnvironment env;

    @Autowired
    Instrumentation instrumentation;

    private static final ClassLoader INSPECTIT_CLASSLOADER = AsyncClassTransformer.class.getClassLoader();

    /**
     * Checks if the given class should not be instrumented
     *
     * @param clazz the class to check
     * @return true, if the class is ignored (=it should not be instrumented)
     */
    public boolean isIgnoredClass(Class<?> clazz) {
        if (!instrumentation.isModifiableClass(clazz)) {
            return true;
        }

        if (clazz.getClassLoader() == INSPECTIT_CLASSLOADER) {
            return true;
        }
        if (clazz.getClassLoader() == null) {
            String name = clazz.getName();
            boolean isIgnored = env.getCurrentConfig().getInstrumentation().getIgnoredBootstrapPackages().entrySet().stream()
                    .filter(e -> e.getValue() == true)
                    .anyMatch(e -> name.startsWith(e.getKey() + "."));
            if (isIgnored) {
                return true;
            }
        }

        return false;
    }
}
