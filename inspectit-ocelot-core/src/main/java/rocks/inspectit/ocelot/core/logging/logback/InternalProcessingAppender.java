package rocks.inspectit.ocelot.core.logging.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.google.common.annotations.VisibleForTesting;
import lombok.NonNull;
import lombok.val;
import org.springframework.context.event.EventListener;
import rocks.inspectit.ocelot.core.config.PropertyNamesValidator;
import rocks.inspectit.ocelot.core.config.PropertySourcesChangedEvent;
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

    private static final Map<Class<? extends Observer>, Observer> observers = new HashMap<>();

    static {
        INVALIDATORS.put(InstrumentationManager.class.getPackage()
                .getName(), InstrumentationConfigurationChangedEvent.class);
        INVALIDATORS.put(PropertyNamesValidator.class.getCanonicalName(), PropertySourcesChangedEvent.class);
        INVALIDATORS.put(EnvironmentInformationPropertySource.class.getPackage()
                .getName(), PropertySourcesChangedEvent.class);
    }

    /**
     * Registers an {@link Observer} to be notified when a new logging event comes in.
     * Following the singleton principle, only one observer per observer class is allowed.
     *
     * @param observer The observer to be called with incoming events
     */
    public static void register(@NonNull Observer observer) {
        observers.put(observer.getClass(), observer);
    }

    /**
     * Unregisters an {@link Observer}, which won't be notified anymore.
     *
     * @param observer The observer to be unregistered
     */
    public static void unregister(@NonNull Observer observer) {
        observers.remove(observer.getClass(), observer);
    }

    @Override
    protected void append(ILoggingEvent event) {
        String key = INVALIDATORS.floorKey(event.getLoggerName());
        Class<?> invalidator = null;

        if (event.getLoggerName().startsWith(key)) {
            invalidator = INVALIDATORS.get(key);
        }

        for (val observer : observers.values()) {
            observer.onLoggingEvent(event, invalidator);
        }
    }

    @EventListener
    private void onInstrumentationConfigurationChangedEvent(InstrumentationConfigurationChangedEvent ev) {
        onInvalidationEvent(ev);
    }

    @EventListener
    private void onPropertySourcesChangedEvent(PropertySourcesChangedEvent ev) {
        onInvalidationEvent(ev);
    }

    @VisibleForTesting
    void onInvalidationEvent(Object ev) {
        for (val observer : observers.values()) {
            observer.invalidateEvents(ev);
        }
    }

    /**
     * Observer to logging events.
     */
    public interface Observer {

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
        default void invalidateEvents(Object invalidator) {
        }

    }
}
