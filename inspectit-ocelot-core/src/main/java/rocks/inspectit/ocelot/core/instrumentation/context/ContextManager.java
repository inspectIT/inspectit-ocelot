package rocks.inspectit.ocelot.core.instrumentation.context;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.grpc.Context;
import io.opencensus.tags.Tags;
import rocks.inspectit.ocelot.bootstrap.context.IContextManager;
import rocks.inspectit.ocelot.core.config.spring.BootstrapInitializerConfiguration;
import rocks.inspectit.ocelot.core.instrumentation.config.InstrumentationConfigurationResolver;
import rocks.inspectit.ocelot.core.tags.CommonTagsManager;

import java.util.concurrent.Callable;

/**
 * This class is based on the ContextStrategyImpl (https://github.com/census-instrumentation/opencensus-java/blob/master/contrib/agent/src/main/java/io/opencensus/contrib/agent/instrumentation/ContextStrategyImpl.java)
 * class from the opencensus-java repository.
 */
public class ContextManager implements IContextManager {

    /**
     * The name of this singleton injected by {@link BootstrapInitializerConfiguration}.
     * Can be used in {@link org.springframework.context.annotation.DependsOn} annotation to ensure correct initialization order.
     */
    public static final String BEAN_NAME = "contextManager";

    private static final boolean IS_OPEN_CENSUS_ON_BOOTSTRAP = Tags.class.getClassLoader() == null;

    private CommonTagsManager commonTagsManager;

    private InstrumentationConfigurationResolver configProvider;

    /**
     * Cache for storing the context objects.
     */
    private final Cache<Thread, Context> storedContexts = CacheBuilder.newBuilder().weakKeys().build();

    public ContextManager(CommonTagsManager commonTagsManager, InstrumentationConfigurationResolver configProvider) {
        this.commonTagsManager = commonTagsManager;
        this.configProvider = configProvider;
    }

    @Override
    public Runnable wrap(Runnable r) {
        return Context.current().wrap(r);
    }

    @Override
    public <T> Callable<T> wrap(Callable<T> callable) {
        return Context.current().wrap(callable);
    }

    @Override
    public void storeContextForThread(Thread thread) {
        storedContexts.put(thread, Context.current());
    }

    @Override
    public void attachContextToThread(Thread thread) {
        Context context = storedContexts.getIfPresent(thread);
        if (context != null) {
            storedContexts.invalidate(thread);
            context.attach();
        }
    }

    @Override
    public InspectitContextImpl enterNewContext() {
        return InspectitContextImpl.createFromCurrent(commonTagsManager.getCommonTagValueMap(), configProvider.getCurrentConfig().getPropagationMetaData(), IS_OPEN_CENSUS_ON_BOOTSTRAP);
    }

}
