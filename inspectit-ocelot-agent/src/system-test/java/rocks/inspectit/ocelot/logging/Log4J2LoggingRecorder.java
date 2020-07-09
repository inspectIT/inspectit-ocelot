package rocks.inspectit.ocelot.logging;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

import java.util.ArrayList;
import java.util.List;

@Plugin(name = "Log4J2LoggingRecorder", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE)
public class Log4J2LoggingRecorder extends AbstractAppender {

    public static final List<LogEvent> loggingEvents = new ArrayList<>();

    protected Log4J2LoggingRecorder(String name, Filter filter) {
        super(name, filter, null, true, Property.EMPTY_ARRAY);
    }

    @Override
    public void append(LogEvent event) {
        loggingEvents.add(event);
    }

    @PluginFactory
    public static Log4J2LoggingRecorder createAppender(@PluginAttribute("name") String name, @PluginElement("Filter") Filter filter) {
        return new Log4J2LoggingRecorder(name, filter);
    }
}
