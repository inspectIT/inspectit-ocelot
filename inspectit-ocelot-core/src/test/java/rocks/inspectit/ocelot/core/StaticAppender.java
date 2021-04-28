package rocks.inspectit.ocelot.core;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import java.util.ArrayList;
import java.util.List;

public class StaticAppender extends AppenderBase<ILoggingEvent> {

    static List<ILoggingEvent> events = new ArrayList<>();

    static final StaticAppender APPENDER = new StaticAppender();

    static final String APPENDER_NAME = "static-appender";

    private StaticAppender() {
        this.setName(APPENDER_NAME);
    }

    @Override
    protected void append(ILoggingEvent e) {
        events.add(e);
    }

    protected static List<ILoggingEvent> getEvents() {
        return events;
    }

    protected static void clearEvents() {
        events.clear();
    }
}