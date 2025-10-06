package rocks.inspectit.ocelot.core.instrumentation.context;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.bootstrap.context.ContextTuple;
import rocks.inspectit.ocelot.bootstrap.context.IContextManager;
import rocks.inspectit.ocelot.core.config.spring.BootstrapInitializerConfiguration;
import rocks.inspectit.ocelot.core.instrumentation.config.InstrumentationConfigurationResolver;
import rocks.inspectit.ocelot.core.instrumentation.context.session.PropagationSessionStorage;
import rocks.inspectit.ocelot.core.attributes.CommonAttributesManager;

import java.util.concurrent.Callable;

import static java.lang.Boolean.TRUE;

/**
 * OpenTelemetry and inspectIT Ocelot use {@link Context} to propagate data, such as the InspectitContext, Span or TraceId.
 */
public class ContextManager implements IContextManager {

    /**
     * The name of this singleton injected by {@link BootstrapInitializerConfiguration}.
     * Can be used in {@link org.springframework.context.annotation.DependsOn} annotation to ensure correct initialization order.
     */
    public static final String BEAN_NAME = "contextManager";

    private static final boolean IS_OPENTELEMETRY_ON_BOOTSTRAP = Attributes.class.getClassLoader() == null;

    private final CommonAttributesManager commonAttributes;

    private final PropagationSessionStorage sessionStorage;

    private final InstrumentationConfigurationResolver configResolver;

    /**
     * Cache for storing the context objects.
     */
    private final Cache<Object, InvalidationContext> contextCache = CacheBuilder.newBuilder().weakKeys().build();

    /**
     * Flag for marking if a context correlation is in progress. See {@link rocks.inspectit.ocelot.core.instrumentation.special.ExecutorContextPropagationSensor}
     * for more details.
     */
    private final ThreadLocal<Boolean> correlationFlag = ThreadLocal.withInitial(() -> false);

    public ContextManager(CommonAttributesManager commonAttributes, PropagationSessionStorage sessionStorage, InstrumentationConfigurationResolver configResolver) {
        this.commonAttributes = commonAttributes;
        this.sessionStorage = sessionStorage;
        this.configResolver = configResolver;
    }

    @Override
    public Runnable wrap(Runnable r) {
        Context current = Context.current();

        return () -> {
            try (Scope ignored = current.makeCurrent()) {
                r.run();
            }
        };
    }

    @Override
    public <T> Callable<T> wrap(Callable<T> callable) {
        Context current = Context.current();

        return () -> {
            try (Scope ignored = current.makeCurrent()) {
                return callable.call();
            }
        };
    }

    @Override
    public InspectitContextImpl enterNewContext() {
        return InspectitContextImpl.createFromCurrent(commonAttributes.getCommonAttributeValueMap(), configResolver.getCurrentConfig()
                .getPropagationMetaData(), sessionStorage, IS_OPENTELEMETRY_ON_BOOTSTRAP);
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
            Scope previous = invalidationContext.attach();

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
         * Attaches the {@link #context} and enters a new scope within the current context.
         * @return the {@link Scope} of {@link Context#makeCurrent()}
         */
        private Scope attach() {
            return context.makeCurrent();
        }
    }

    /**
     * {@link ContextTuple} implementation used by {@link #detachContext(ContextTuple)} for detaching a context.
     */
    @AllArgsConstructor
    private static class ContextTupleImpl implements ContextTuple {

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
         * {@link AutoCloseable} for undoing the trace id injection into the logging MDCs.
         */
        @NonNull
        private final AutoCloseable undoTraceInjection;

        /**
         * Restores the {@link #previous}
         */
        private void detach() {
            // close the OTEL scope to restore the previous OTEL context
            previous.close();
        }
    }

}
