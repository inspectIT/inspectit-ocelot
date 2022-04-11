package rocks.inspectit.ocelot.core.logging.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.google.common.annotations.VisibleForTesting;
import lombok.NonNull;
import lombok.val;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.config.PropertyNamesValidator;
import rocks.inspectit.ocelot.core.config.PropertySourcesReloadEvent;
import rocks.inspectit.ocelot.core.config.propertysources.EnvironmentInformationPropertySource;
import rocks.inspectit.ocelot.core.instrumentation.InstrumentationManager;
import rocks.inspectit.ocelot.core.instrumentation.config.event.InstrumentationConfigurationChangedEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Logback appender that forwards the events to observers for internal processing.
 */
public class InternalProcessingAppender extends AppenderBase<ILoggingEvent> {

    private static final NavigableMap<String, Class<?>> INVALIDATORS = new TreeMap<>();

    private static final Map<Class<? extends LogEventConsumer>, LogEventConsumer> consumers = new HashMap<>();

    static {
        INVALIDATORS.put(InstrumentationManager.class.getPackage()
                .getName(), InstrumentationConfigurationChangedEvent.class);
        INVALIDATORS.put(InspectitEnvironment.class.getCanonicalName(), PropertySourcesReloadEvent.class);
        INVALIDATORS.put(PropertyNamesValidator.class.getCanonicalName(), PropertySourcesReloadEvent.class);
        INVALIDATORS.put(EnvironmentInformationPropertySource.class.getPackage()
                .getName(), PropertySourcesReloadEvent.class);
    }

    /**
     * Registers an {@link LogEventConsumer} to be notified when a new logging event comes in.
     * Following the singleton principle, only one observer per observer class is allowed.
     *
     * @param consumer The observer to be called with incoming events
     */
    public static void register(@NonNull InternalProcessingAppender.LogEventConsumer consumer) {
        consumers.put(consumer.getClass(), consumer);
    }

    /**
     * Unregisters an {@link LogEventConsumer}, which won't be notified anymore.
     *
     * @param consumer The observer to be unregistered
     */
    public static void unregister(@NonNull InternalProcessingAppender.LogEventConsumer consumer) {
        consumers.remove(consumer.getClass(), consumer);
    }

    @Override
    protected void append(ILoggingEvent event) {
        String key = INVALIDATORS.floorKey(event.getLoggerName());
        Class<?> invalidator = null;

        if (key != null && event.getLoggerName().startsWith(key)) {
            invalidator = INVALIDATORS.get(key);
        }

        for (val observer : consumers.values()) {
            observer.onLoggingEvent(event, invalidator);
        }
    }

    @VisibleForTesting
    static void onInvalidationEvent(Object ev) {
        for (val observer : consumers.values()) {
            observer.onInvalidationEvent(ev);
        }
    }

    @Component
    static class InvalidationListener {

        @EventListener
        @Order(Ordered.HIGHEST_PRECEDENCE)
        private void onInstrumentationConfigurationChangedEvent(InstrumentationConfigurationChangedEvent ev) {
            onInvalidationEvent(ev);
        }

        @EventListener
        @Order(Ordered.HIGHEST_PRECEDENCE)
        private void onPropertySourcesReloadEvent(PropertySourcesReloadEvent ev) {
            onInvalidationEvent(ev);
        }

    }

    /**
     * Consumer of logging events.
     * When registered using {@link InternalProcessingAppender#register(LogEventConsumer)}, it will be called
     * when new log events occur {@link #onLoggingEvent(ILoggingEvent, Class)} or log events are invalidated
     * {@link #onInvalidationEvent(Object)}.
     */
    public interface LogEventConsumer {

        /**
         * Will be called whenever a new logging event comes in.
         *
         * @param event       The logging event
         * @param invalidator Class whose instances invalidate the log event when being sent as event, e.g., {@link rocks.inspectit.ocelot.core.config.InspectitConfigChangedEvent}. May be null.
         */
        void onLoggingEvent(ILoggingEvent event, Class<?> invalidator);

        /**
         * Will be called whenever an invalidating event occurred.
         *
         * @param invalidator The invalidator
         */
        default void onInvalidationEvent(Object invalidator) {
        }

    }
}
