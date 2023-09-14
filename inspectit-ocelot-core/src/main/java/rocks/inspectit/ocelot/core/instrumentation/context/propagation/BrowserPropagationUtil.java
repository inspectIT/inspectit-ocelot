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
    private static String sessionIdKey;

    @PostConstruct
    public void initialize() {
        setSessionIdKey(env.getCurrentConfig().getExporters().getTags().getHttp().getSessionIdKey());
    }

    @EventListener
    private void configEventListener(InspectitConfigChangedEvent event) {
        String oldSessionIdKey = event.getOldConfig().getExporters().getTags().getHttp().getSessionIdKey();
        String newSessionIdKey = event.getNewConfig().getExporters().getTags().getHttp().getSessionIdKey();

        if(!oldSessionIdKey.equals(newSessionIdKey)) setSessionIdKey(newSessionIdKey);
    }

    @VisibleForTesting
    void setSessionIdKey(String key) {
        sessionIdKey = key;
        ContextPropagationUtil.setSessionIdKey(sessionIdKey);
        log.info("Use of new session-id-key: " + sessionIdKey);
    }
}
