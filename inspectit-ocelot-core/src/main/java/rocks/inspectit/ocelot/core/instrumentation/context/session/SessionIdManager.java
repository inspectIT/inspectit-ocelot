package rocks.inspectit.ocelot.core.instrumentation.context.session;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.core.config.InspectitConfigChangedEvent;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.instrumentation.context.propagation.ContextPropagation;

import javax.annotation.PostConstruct;


/**
 * This component only exists for listening on configuration changes and correctly setting the session-id header
 * in the {@link ContextPropagation}.
 */
@Slf4j
@Component
public class SessionIdManager {

    @Autowired
    private InspectitEnvironment env;

    @PostConstruct
    public void postConstruct() {
        String initialSessionIdHeader = env.getCurrentConfig().getInstrumentation().getSessions().getSessionIdHeader();
        setSessionIdHeader(initialSessionIdHeader);
    }

    @EventListener
    private void configEventListener(InspectitConfigChangedEvent event) {
        String oldSessionIdHeader =  event.getOldConfig().getInstrumentation().getSessions().getSessionIdHeader();
        String newSessionIdHeader = event.getNewConfig().getInstrumentation().getSessions().getSessionIdHeader();

        if(!oldSessionIdHeader.equals(newSessionIdHeader)) setSessionIdHeader(newSessionIdHeader);
    }

    @VisibleForTesting
    void setSessionIdHeader(String header) {
        if (header != null && !header.isEmpty()) {
            log.info("Using new session-id header: {}", header);
            ContextPropagation.get().setSessionIdHeader(header);
        }
    }
}
