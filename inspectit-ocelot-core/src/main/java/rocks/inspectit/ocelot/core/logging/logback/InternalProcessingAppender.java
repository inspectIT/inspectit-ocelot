package rocks.inspectit.ocelot.core.logging.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import lombok.NonNull;
import lombok.val;

import java.util.ArrayList;
import java.util.List;

/**
 * Logback appender that forwards the events to observers for internal processing.
 */
public class InternalProcessingAppender extends AppenderBase<ILoggingEvent> {

    private static final List<Observer> observers = new ArrayList<>();

    /**
     * Registers an {@link Observer} to be notified when a new logging event comes in.
     *
     * @param observer The observer, whose {@link Observer#onLoggingEvent(ILoggingEvent, Category) onLoggingEvent(...)} method sshould be called
     */
    public static void register(@NonNull Observer observer) {
        observers.add(observer);
    }

    @Override
    protected void append(ILoggingEvent event) {
        for (val observer : observers) {
            observer.onGeneralLoggingEvent(event); // TODO: select category
        }
    }

    /**
     * Observer to logging events.
     */
    public interface Observer {

        /**
         * Will be called whenever a new logging event related to the instrumentation process comes in.
         *
         * @param event The logging event
         */
        void onInstrumentationLoggingEvent(ILoggingEvent event);

        /**
         * Will be called whenever a new general logging event comes in.
         *
         * @param event The logging event
         */
        void onGeneralLoggingEvent(ILoggingEvent event);

    }
}
