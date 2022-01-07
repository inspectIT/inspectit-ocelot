package rocks.inspectit.ocelot.core.instrumentation.context;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import rocks.inspectit.ocelot.bootstrap.exposed.InspectitContext;

/**
 * Utility class to get the current OTEL {@link io.opentelemetry.context.Context} and GRPC {@link io.grpc.Context}.
 * <p>
 * This utility class is a response to the change from {@link io.grpc.Context} to {@link io.opentelemetry.context.Context} in OpenTelemetry.
 */
public class ContextUtil {

    /**
     * Gets the current OTEL {@link Context} via {@link Context#current()}
     *
     * @return The current OTEL {@link Context} via {@link Context#current()}
     */
    public static io.opentelemetry.context.Context current() {
        return io.opentelemetry.context.Context.current();
    }

    /**
     * Gets the current GRPC {@link io.grpc.Context} via {@link io.grpc.Context#current()}
     *
     * @return The current GRPC {@link io.grpc.Context} via {@link io.grpc.Context#current()}
     */
    public static io.grpc.Context currentGrpc() {
        return io.grpc.Context.current();
    }

    /**
     * Gets the {@link InspectitContext} stored in the {@link #current() current OTEL context} with the {@link InspectitContextImpl#INSPECTIT_KEY}
     *
     * @return The {@link InspectitContext} stored in the {@link #current() current OTEL context} with the {@link InspectitContextImpl#INSPECTIT_KEY}
     */
    public static InspectitContextImpl currentInspectitContext() {
        return current().get(InspectitContextImpl.INSPECTIT_KEY);
    }

    /**
     * Gets the {@link InspectitContext} stored in the {@link #currentGrpc() current GRPC context} with the {@link InspectitContextImpl#INSPECTIT_KEY_GRPC}
     *
     * @return The {@link InspectitContext} stored in the {@link #currentGrpc() current GRPC context} with the {@link InspectitContextImpl#INSPECTIT_KEY_GRPC}
     */
    public static InspectitContextImpl currentInspectitContextStoredInGrpcContext() {
        return InspectitContextImpl.INSPECTIT_KEY_GRPC.get(currentGrpc());
    }

    /**
     * Gets the object stored in {@link #current()} with the given {@link ContextKey}
     *
     * @param key The {@link ContextKey} under which the object is stored
     * @param <T> The type of the object
     *
     * @return The object stored under {@link #current()} with the given {@link ContextKey}
     */
    public static <T> T get(ContextKey<T> key) {
        return current().get(key);
    }
}
