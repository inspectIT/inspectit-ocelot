package rocks.inspectit.ocelot.core.instrumentation.injection;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.bootstrap.instrumentation.DoNotInstrumentMarker;

import javax.annotation.PostConstruct;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * When using Java9+, classes are bundled in modules with access restrictions.
 * This class can be used to redefine the modules in order to make them accessible to inspectIT without {@code illegal access} warnings.
 */
@Component
@Slf4j
public class JigsawModuleInstrumenter {

    /**
     * Reference to Class.getModule() (introduced in Java 9)
     */
    private Method classGetModule;

    private Method moduleGetPackages;

    private Method moduleIsNamed;

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
     * Caches which modules have already been opened.
     */
    private final Cache<Object, Boolean> enhancedModules = CacheBuilder.newBuilder().weakKeys().build();

    @Autowired
    private Instrumentation instrumentation;

    /**
     * Initialization.
     *
     * @throws NoSuchMethodException If the Java9 methods do not exists, but java.lang.Module does. Should therefore never happen.
     */
    @PostConstruct
    void init() throws NoSuchMethodException {
        Class<?> moduleClass;
        try {
            moduleClass = Class.forName("java.lang.Module");
        } catch (ClassNotFoundException e) {
            log.debug("'java.lang.Module' not found, assuming Java 8");
            return;
        }

        try {
            classGetModule = Class.class.getMethod("getModule");
            moduleGetPackages = moduleClass.getMethod("getPackages");
            moduleIsNamed = moduleClass.getMethod("isNamed");
            instrumentationRedefineModule = Instrumentation.class.getMethod("redefineModule", moduleClass, Set.class, Map.class, Map.class, Set.class, Map.class);
        } catch (NoSuchMethodException e) {
            log.error("Your JRE contains java.lang.Module but not the required methods!", e);
            throw e;
        }

        bootstrapModule = getModuleOfClass(DoNotInstrumentMarker.class);
        coreModule = getModuleOfClass(JigsawModuleInstrumenter.class);
    }

    /**
     * Redefines all modules to ensure modules can access boostrap and core inspectIT modules.
     * Extra reads and writes are added to modules by utilizing Instrumentation.redefineModule()
     *
     * @param module The module to be redefined
     */
    public synchronized void openModule(Object module) {
        if (isModuleSystemAvailable()) {
            if (enhancedModules.getIfPresent(module) == null) {
                Set<String> packages = getPackagesOfModule(module);
                if (isNamed(module)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Gaining access to package '{}' of module '{}'", StringUtils.join(packages, ", "), module);
                    }
                    Set<Object> extraReads = Collections.singleton(bootstrapModule);
                    Map<String, Set<Object>> extraOpens = packages.stream()
                            .collect(Collectors.toMap(Object::toString, x -> Collections.singleton(coreModule)));
                    redefineModule(module, extraReads, extraOpens);
                }
                enhancedModules.put(module, true);
            }

        }
    }

    /**
     * Instruments the module containing the given class in order to gain the required access rights.
     * Is a NOOP if the Java Version is 8.
     *
     * @param containedClass the class whose containing module should be instrumented
     */
    public synchronized void openModule(Class<?> containedClass) {
        if (isModuleSystemAvailable()) {
            Object module = getModuleOfClass(containedClass);
            openModule(module);
        }
    }

    /**
     * Invokes Instrumentation.redefineModule, which has been introduced in Java 9.
     * <p>
     * Any kind of thrown exceptions are caught, logged and ignored.
     *
     * @param module     The module to redefine
     * @param extraReads The set of Module to add as readable
     * @param extraOpens Maps packages to modules to which these packages shall be run-time visible (E.g. via deep reflection)
     */
    private void redefineModule(Object module, Set<Object> extraReads, Map<String, Set<Object>> extraOpens) {
        try {
            instrumentationRedefineModule.invoke(instrumentation, module, extraReads, Collections.emptyMap(), extraOpens, Collections.emptySet(), Collections.emptyMap());
        } catch (Exception e) {
            log.error("Error redefining module {}", module, e);
        }
    }

    /**
     * Invokes the java 9 Class.getModule() function (never returns null)
     *
     * @param clazz the class to query the module of
     *
     * @return the module of the given class, never null
     */
    private Object getModuleOfClass(Class<?> clazz) {
        try {
            return classGetModule.invoke(clazz);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Invokes the java 9 Module.isNamed() function
     *
     * @param module The Module instance
     *
     * @return true if it is a named module, false otherwise
     */
    private boolean isNamed(Object module) {
        try {
            return (boolean) moduleIsNamed.invoke(module);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Invokes the java 9 Module.getPackages() function
     *
     * @param module The Module instance
     *
     * @return Returns the set of package names for the packages in this module.
     */
    private Set<String> getPackagesOfModule(Object module) {
        try {
            Set<String> packages = (Set<String>) moduleGetPackages.invoke(module);
            return packages;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private boolean isModuleSystemAvailable() {
        return classGetModule != null;
    }

}
