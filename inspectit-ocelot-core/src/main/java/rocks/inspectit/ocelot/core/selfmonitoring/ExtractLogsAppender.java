package rocks.inspectit.ocelot.core.selfmonitoring;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import java.util.ArrayList;
import java.util.List;

public class ExtractLogsAppender extends AppenderBase<ILoggingEvent> {
    public static List<ILoggingEvent> list = new ArrayList<>();

    @Override
    protected void append(ILoggingEvent logEvent) {
        list.add(logEvent);
    }
}