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

    private Method classGetModule;
    private Method instrumentationRedefineModule;

    private Object bootstrapModule;
    private Object coreModule;

    private WeakHashMap<Object, Set<String>> enhancedModules = new WeakHashMap<>();

    @Autowired
    Instrumentation instrumentation;

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
            throw new IllegalArgumentException(e);
        }
    }

    private Object getModuleOfClass(Class<?> clazz) {
        try {
            return classGetModule.invoke(clazz);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean isModuleSystemAvailable() {
        return classGetModule != null;
    }

}
