package rocks.inspectit.ocelot.logging;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

import java.util.ArrayList;
import java.util.List;

public class Log4JLoggingRecorder extends AppenderSkeleton {

    public static final List<LoggingEvent> loggingEvents = new ArrayList<>();

    @Override
    protected void append(LoggingEvent event) {
        loggingEvents.add(event);
    }

    @Override
    public void close() {

    }

    @Override
    public boolean requiresLayout() {
        return false;
    }
}
