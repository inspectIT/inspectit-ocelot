package rocks.inspectit.ocelot.core.testutils;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.test.context.event.ApplicationEvents;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Helper class to check for specific published {@link ApplicationEvent application events}.
 * We could have used the Spring {@link ApplicationEvents} as well, if we would use {@link SpringBootTest}.
 */
@Component
public class TestEventListener implements ApplicationListener<ApplicationEvent> {

    /**
     * All collected application events.
     * Should be cleared automatically after each test, because the bean will be re-initialized.
     */
    private final List<ApplicationEvent> events = new LinkedList<>();

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        events.add(event);
    }

    public <T extends ApplicationEvent> List<T> getEvents(Class<T> eventType) {
        return events.stream()
                .filter(eventType::isInstance)
                .map(eventType::cast)
                .collect(Collectors.toList());
    }
}
