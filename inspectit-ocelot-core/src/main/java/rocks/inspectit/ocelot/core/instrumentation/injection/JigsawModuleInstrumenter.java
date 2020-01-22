package rocks.inspectit.ocelot.core.instrumentation.injection;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.bootstrap.instrumentation.DoNotInstrumentMarker;

import javax.annotation.PostConstruct;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.*;

@Component
@Slf4j
public class JigsawModuleInstrumenter {

    /**
     * Reference to Class.getModule() (introduced in Java 9)
     */
    private Method classGetModule;

    /**
     * Reference to Instrumentation.redefineModule (introduced in Java 9)
     */
    private Method instrumentationRedefineModule;

    /**
     * The (unnamed) module containing all Ocelot bootstrap classes.
     */
    private Object bootstrapModule;

    /**
     * The (unnamed) module containing all Ocelot core classes.
     */
    private Object coreModule;

    /**
     * Maps modules to the set of packages which have already been opened to the {@link #coreModule}.
     * Prevents unnecessary duplicate invocations of {@link #instrumentationRedefineModule}
     */
    private WeakHashMap<Object, Set<String>> enhancedModules = new WeakHashMap<>();

    @Autowired
    private Instrumentation instrumentation;

    @PostConstruct
    void init() throws NoSuchMethodException {
        Class<?> moduleClass;
        try {
            moduleClass = Class.forName("java.lang.Module");
        } catch (ClassNotFoundException e) {
            log.debug("'java.lang.Module' not found, assuming Java 8");
            return;
        }

        classGetModule = Class.class.getMethod("getModule");
        instrumentationRedefineModule = Instrumentation.class.getMethod("redefineModule", moduleClass, Set.class, Map.class, Map.class, Set.class, Map.class);

        bootstrapModule = getModuleOfClass(DoNotInstrumentMarker.class);
        coreModule = getModuleOfClass(JigsawModuleInstrumenter.class);
    }

    public synchronized void openModule(Class<?> containedClass) {
        if (isModuleSystemAvailable()) {
            String packageName = containedClass.getPackage().getName();
            Object module = getModuleOfClass(containedClass);
            Set<String> openedPackages = enhancedModules.computeIfAbsent(module, (m) -> new HashSet<>());
            if (!openedPackages.contains(packageName)) {
                log.debug("Gaining access to package '{}' of module '{}'", packageName, module);
                Set<Object> extraReads = Collections.singleton(bootstrapModule);
                Map<String, Set<Object>> extraOpens = Collections.singletonMap(packageName, Collections.singleton(coreModule));
                redefineModule(module, extraReads, Collections.emptyMap(), extraOpens, Collections.emptySet(), Collections.emptyMap());
                openedPackages.add(packageName);
            }
        }
    }

    /**
     * Invokes Instrumentation.redefineModule, which has been introduced in Java 9.
     * <p>
     * Any kind of thrown exceptions are caught, logged and ignored.
     *
     * @param module        The module to redefine
     * @param extraReads    The set of Module to add as readable
     * @param extraExports  Maps packages to modules to which these packages shall be compile-time visible
     * @param extraOpens    Maps packages to modules to which these packages shall be run-time visible (E.g. via deep reflection)
     * @param extraUses     The additional services to use by this module
     * @param extraProvides The additional services to expose by this module
     */
    private void redefineModule(Object module,
                                Set<Object> extraReads,
                                Map<String, Set<Object>> extraExports,
                                Map<String, Set<Object>> extraOpens,
                                Set<Class<?>> extraUses,
                                Map<Class<?>, List<Class<?>>> extraProvides) {
        try {
            instrumentationRedefineModule.invoke(instrumentation,
                    module,
                    extraReads,
                    extraExports,
                    extraOpens,
                    extraUses,
                    extraProvides
            );
        } catch (Exception e) {
            log.error("Error redefining module {}", module, e);
        }
    }

    /**
     * Invokes the java 9 Class.getModule() function (never returns null)
     *
     * @param clazz the class to query the module of
     * @return the module of the given class, never null
     */
    private Object getModuleOfClass(Class<?> clazz) {
        try {
            return classGetModule.invoke(clazz);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private boolean isModuleSystemAvailable() {
        return classGetModule != null;
    }

}
