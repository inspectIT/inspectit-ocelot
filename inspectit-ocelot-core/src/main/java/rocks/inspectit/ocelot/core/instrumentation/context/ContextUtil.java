package rocks.inspectit.ocelot.core.instrumentation.context;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import rocks.inspectit.ocelot.bootstrap.exposed.InspectitContext;

/**
 * Utility class to get the current OTEL {@link io.opentelemetry.context.Context}
 */
public class ContextUtil {

    /**
     * Gets the {@link InspectitContext} stored in the current OTel {@link Context context}
     * with the {@link InspectitContextImpl#INSPECTIT_KEY}.
     *
     * @return the {@link InspectitContext} stored in the current OTel {@link Context context}
     */
    public static InspectitContextImpl currentInspectitContext() {
        return Context.current().get(InspectitContextImpl.INSPECTIT_KEY);
    }

    /**
     * Gets the {@link InspectitContext} stored in the current OTel {@link Context context} with the given {@link ContextKey}
     *
     * @param key The {@link ContextKey} under which the object is stored
     * @param <T> The type of the object
     *
     * @return the object stored under the current OTel {@link Context context} with the given {@link ContextKey}
     */
    public static <T> T getObject(ContextKey<T> key) {
        return Context.current().get(key);
    }
}
