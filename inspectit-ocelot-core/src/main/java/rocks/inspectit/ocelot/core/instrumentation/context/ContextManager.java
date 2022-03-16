package rocks.inspectit.ocelot.core.instrumentation.context;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.opencensus.tags.Tags;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.apache.commons.lang3.tuple.Pair;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.bootstrap.context.ContextTuple;
import rocks.inspectit.ocelot.bootstrap.context.IContextManager;
import rocks.inspectit.ocelot.core.config.spring.BootstrapInitializerConfiguration;
import rocks.inspectit.ocelot.core.instrumentation.config.InstrumentationConfigurationResolver;
import rocks.inspectit.ocelot.core.tags.CommonTagsManager;

import javax.validation.constraints.NotNull;
import java.util.concurrent.Callable;

import static java.lang.Boolean.TRUE;

/**
 * This class is based on the ContextStrategyImpl (https://github.com/census-instrumentation/opencensus-java/blob/master/contrib/agent/src/main/java/io/opencensus/contrib/agent/instrumentation/ContextStrategyImpl.java)
 * class from the opencensus-java repository.
 *
 * OpenCensus and inspectIT Ocelot use {@link io.grpc.Context} to propagate data, such as the InspectitContext, Span or TraceId.
 * OpenTelemetry uses its own {@link io.opentelemetry.context.Context} implementation.
 * To make all system tests succeed, we need to concurrently support (re-)storing data in {@link io.grpc.Context} as well as {@link io.opentelemetry.context.Context}.
 *
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
        // manually build up own wrap method to support io.grpc.Context and io.telemetry.context.Context
        // we need to support both context implementations for the transition from OpenCensus to OpenTelemetry, see https://github.com/inspectIT/inspectit-ocelot/pull/1270#issuecomment-1010061201

        // get current OpenTelemetry and grpc context
        Context current = ContextUtil.current();
        io.grpc.Context currentGrpc = ContextUtil.currentGrpc();

        return () -> {
            // attach current grpc context to obtain the previous context
            io.grpc.Context previousGrpc = currentGrpc.attach();
            // wrap runnable in OpenTelemetry context
            try (Scope ignored = current.makeCurrent()) {
                r.run();
            } finally {
                // restore the previous grpc context
                currentGrpc.detach(previousGrpc);
            }
        };
    }

    @Override
    public <T> Callable<T> wrap(Callable<T> callable) {
        // manually build up own wrap method to support io.grpc.Context and io.telemetry.context.Context

        // get current OpenTelemetry and grpc context
        Context current = ContextUtil.current();
        io.grpc.Context currentGrpc = ContextUtil.currentGrpc();

        return () -> {
            // attach current grpc context to obtain the previous context
            io.grpc.Context previousGrpc = currentGrpc.attach();
            try (Scope ignored = current.makeCurrent()) {
                return callable.call();
            } finally {
                // restore the previous grpc context
                currentGrpc.detach(previousGrpc);
            }
        };
    }

    @Override
    public InspectitContextImpl enterNewContext() {
        return InspectitContextImpl.createFromCurrent(commonTagsManager.getCommonTagValueMap(), configProvider.getCurrentConfig()
                .getPropagationMetaData(), IS_OPEN_CENSUS_ON_BOOTSTRAP);
    }

    @Override
    public void storeContext(Object target, boolean invalidateAfterRestoring) {
        InvalidationContext invalidationContext = new InvalidationContext(invalidateAfterRestoring, ContextUtil.current(), ContextUtil.currentGrpc());
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
            Pair<Scope, io.grpc.Context> previous = invalidationContext.attach();

            // once the context is attached, we inject the trace id into the MDCs for log-trace correlation
            AutoCloseable undoTraceInjection = Instances.logTraceCorrelator.injectTraceIdIntoMdc();

            // data we need once the method exits in order to undo the previous changes
            return new ContextTupleImpl(previous.getLeft(), invalidationContext.context, previous.getRight(), invalidationContext.contextGrpc, undoTraceInjection);
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
            tuple.detach();
        }
    }

    @Override
    public boolean enterCorrelation() {
        if (TRUE.equals(correlationFlag.get())) {
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
        private boolean invalidate;

        /**
         * The {@link io.opentelemetry.context.Context} used by OpenTelemetry
         */
        @NonNull
        private Context context;

        /**
         * The {@link io.grpc.Context} used by OpenCensus
         */
        @NotNull
        private io.grpc.Context contextGrpc;

        /**
         * Attaches the {@link #context} and {@link #contextGrpc} and enters a new scope within the current context.
         * @return The {@link Pair} containing the {@link Scope} of {@link Context#makeCurrent()} and the attached {@link io.grpc.Context} of {@link io.grpc.Context#attach()}
         */
        private Pair<Scope, io.grpc.Context> attach() {
            return Pair.of(context.makeCurrent(), contextGrpc.attach());
        }
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
        private final Scope previous;

        /**
         * The context which has been attached.
         */
        @NonNull
        private final Context current;

        /**
         * The previous {@link io.grpc.Context} used by OpenCensus
         */
        @NotNull
        private final io.grpc.Context previousGrpc;

        /**
         * The {@link io.grpc.Context} used by OpenCensus which has been attached
         */
        @NotNull
        private final io.grpc.Context currentGrpc;

        /**
         * {@link AutoCloseable} for undoing the trace id injection into the logging MDCs.
         */
        @NonNull
        private final AutoCloseable undoTraceInjection;

        /**
         * Restores the previous {@link #previous} and {@link #previousGrpc}
         */
        private void detach() {
            // close the OTEL scope to restore the previous OTEL context
            previous.close();
            // restore the previous GRPC context
            currentGrpc.detach(previousGrpc);
        }
    }

}
