package rocks.inspectit.oce.core.tags.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rocks.inspectit.oce.core.config.model.InspectitConfig;
import rocks.inspectit.oce.core.config.model.tags.providers.EnvironmentTagsProviderSettings;
import rocks.inspectit.oce.core.tags.ICommonTagsProvider;

import javax.validation.Valid;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link ICommonTagsProvider} that provides the environment based tags like service name, host, etc.
 */
@Component
@Slf4j
public class EnvironmentTagsProvider implements ICommonTagsProvider {

    @Override
    public Map<String, String> getTags(InspectitConfig configuration) {
        @Valid EnvironmentTagsProviderSettings conf = configuration.getTags().getProviders().getEnvironment();
        if (conf.isEnabled()) {
            Map<String, String> envTags = new HashMap<>();
            envTags.put("service", configuration.getServiceName());

            if (conf.isResolveHostName()) {
                try {
                    envTags.put("host", resolveHostName());
                } catch (UnknownHostException e) {
                    if (log.isDebugEnabled()) {
                        log.debug("Failed to resolve host name.", e);
                    }
                }
            }

            if (conf.isResolveHostAddress()) {
                try {
                    envTags.put("host_address", resolveHostAddress());
                } catch (UnknownHostException e) {
                    if (log.isDebugEnabled()) {
                        log.debug("Failed to resolve host address.", e);
                    }
                }
            }

            return envTags;
        }
        return Collections.emptyMap();
    }

    private static String resolveHostName() throws UnknownHostException {
        return InetAddress.getLocalHost().getHostName();
    }

    private static String resolveHostAddress() throws UnknownHostException {
        return InetAddress.getLocalHost().getHostAddress();
    }

}
