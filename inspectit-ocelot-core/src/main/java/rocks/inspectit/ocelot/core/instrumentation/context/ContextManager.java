package rocks.inspectit.ocelot.core.instrumentation.context;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.grpc.Context;
import io.opencensus.tags.Tags;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.bootstrap.context.ContextTuple;
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
    private final Cache<Object, InvalidationContext> contextCache = CacheBuilder.newBuilder().weakKeys().build();

    /**
     * Flag for marking if a context correlation is in progress. See {@link rocks.inspectit.ocelot.core.instrumentation.special.ExecutorContextPropagationSensor}
     * for more details.
     */
    private final ThreadLocal<Boolean> correlationFlag = ThreadLocal.withInitial(() -> false);

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
    public InspectitContextImpl enterNewContext() {
        return InspectitContextImpl.createFromCurrent(commonTagsManager.getCommonTagValueMap(), configProvider.getCurrentConfig().getPropagationMetaData(), IS_OPEN_CENSUS_ON_BOOTSTRAP);
    }

    @Override
    public void storeContext(Object target, boolean invalidateAfterRestoring) {
        InvalidationContext invalidationContext = new InvalidationContext(invalidateAfterRestoring, Context.current());
        contextCache.put(target, invalidationContext);
    }

    @Override
    public ContextTuple attachContext(Object target) {
        InvalidationContext invalidationContext = contextCache.getIfPresent(target);
        if (invalidationContext != null) {
            if (invalidationContext.invalidate) {
                contextCache.invalidate(target);
            }
            // restore/attach context to current runtime/thread
            Context previous = invalidationContext.context.attach();

            // once the context is attached, we inject the trace id into the MDCs for log-trace correlation
            AutoCloseable undoTraceInjection = Instances.logTraceCorrelator.injectTraceIdIntoMdc();

            // data we need once the method exits in order to undo the previous changes
            return new ContextTupleImpl(previous, invalidationContext.context, undoTraceInjection);
        }
        return null;
    }

    @Override
    public void detachContext(ContextTuple contextTuple) {
        if (contextTuple != null) {
            ContextTupleImpl tuple = (ContextTupleImpl) contextTuple;

            // restore previous MDC content
            try {
                tuple.undoTraceInjection.close();
            } catch (Exception ignored) {
            }

            // restore previous context
            tuple.current.detach(tuple.previous);
        }
    }

    @Override
    public boolean enterCorrelation() {
        if (correlationFlag.get()) {
            return false;
        } else {
            correlationFlag.set(true);
            return true;
        }
    }

    @Override
    public boolean insideCorrelation() {
        return correlationFlag.get();
    }

    @Override
    public void exitCorrelation() {
        correlationFlag.set(false);
    }

    /**
     * Container class for storing contexts in the {@link #contextCache}.
     */
    @AllArgsConstructor
    private class InvalidationContext {

        /**
         * Whether the context should be removed from the context cache after it has been restored.
         */
        private final boolean invalidate;

        @NonNull
        private final Context context;
    }

    /**
     * {@link ContextTuple} implementation used by {@link #detachContext(ContextTuple)} for detaching a context.
     */
    @AllArgsConstructor
    private class ContextTupleImpl implements ContextTuple {

        /**
         * The previous context.
         */
        @NonNull
        private final Context previous;

        /**
         * The context which has been attached.
         */
        @NonNull
        private final Context current;

        /**
         * {@link AutoCloseable} for undoing the trace id injection into the logging MDCs.
         */
        @NonNull
        private final AutoCloseable undoTraceInjection;
    }
}
