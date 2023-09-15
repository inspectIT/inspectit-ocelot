package rocks.inspectit.ocelot.core.instrumentation.context.propagation;

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
 * The session-id-key is used to extract session-ids from http-request-headers to allow browser propagation.
 * The session-id-key can change during runtime and needs to updated inside the PROPAGATION_FIELDS in ContextPropagationUtil.
 */
@Slf4j
@Component
public class BrowserPropagationUtil {

    @Autowired
    private InspectitEnvironment env;
    @Getter
    private static String sessionIdHeader;

    @PostConstruct
    public void initialize() {
        setSessionIdHeader(env.getCurrentConfig().getExporters().getTags().getHttp().getSessionIdHeader());
    }

    @EventListener
    private void configEventListener(InspectitConfigChangedEvent event) {
        String oldSessionIdHeader = event.getOldConfig().getExporters().getTags().getHttp().getSessionIdHeader();
        String newSessionIdHeader = event.getNewConfig().getExporters().getTags().getHttp().getSessionIdHeader();

        if(!oldSessionIdHeader.equals(newSessionIdHeader)) ContextPropagationUtil.setSessionIdHeader(newSessionIdHeader);
    }

    @VisibleForTesting
    void setSessionIdHeader(String key) {
        sessionIdHeader = key;
        log.info("Use of new session-id-header: " + sessionIdHeader);
        ContextPropagationUtil.setSessionIdHeader(sessionIdHeader);
    }
}
