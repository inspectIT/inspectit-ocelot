package rocks.inspectit.ocelot.core.instrumentation.context.session;

import com.google.common.annotations.VisibleForTesting;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.core.config.InspectitConfigChangedEvent;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.instrumentation.context.ContextPropagationUtil;

import javax.annotation.PostConstruct;

/**
 * Class to regulate the currently used session-id-key.
 * The session-id-key is used to extract session-ids from http-headers.
 * The session-id-key can change during runtime and needs to be updated inside the PROPAGATION_FIELDS in ContextPropagationUtil.
 */
@Slf4j
@Component
// TODO This is not a util...
public class BrowserPropagationUtil {

    @Autowired
    private InspectitEnvironment env;

    @Getter
    private static String sessionIdHeader = "Session-Id";

    @PostConstruct
    public void initialize() {
        setSessionIdHeader(env.getCurrentConfig().getInstrumentation().getSessions().getSessionIdHeader());
    }

    @EventListener
    private void configEventListener(InspectitConfigChangedEvent event) {
         String newSessionIdHeader = event.getNewConfig().getInstrumentation().getSessions().getSessionIdHeader();
         if(newSessionIdHeader != null && !newSessionIdHeader.equals(sessionIdHeader)) setSessionIdHeader(newSessionIdHeader);
    }

    @VisibleForTesting
    void setSessionIdHeader(String key) {
        sessionIdHeader = key;
        log.info("Using new session-id header: {}", sessionIdHeader);
        ContextPropagationUtil.setSessionIdHeader(sessionIdHeader);
    }
}
