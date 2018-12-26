package rocks.inspectit.oce.core.tags.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rocks.inspectit.oce.core.config.model.InspectitConfig;
import rocks.inspectit.oce.core.tags.AbstractTagsProvider;
import rocks.inspectit.oce.core.tags.ITagsProvider;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link ITagsProvider} that provides the environment based tags like service name, host, etc.
 */
@Component
@Slf4j
public class EnvironmentTagsProvider extends AbstractTagsProvider {

    /**
     * Default constructor.
     */
    public EnvironmentTagsProvider() {
        super("serviceName", "tags.providers.environment");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean checkEnabledForConfig(InspectitConfig conf) {
        return conf.getTags().getProviders().getEnvironment().isEnabled();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPriority() {
        return Priority.LOW.getValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Map<String, String> resolveTagsInternal(InspectitConfig configuration) {
        Map<String, String> envTags = new HashMap<>();
        envTags.put("service-name", configuration.getServiceName());

        if (configuration.getTags().getProviders().getEnvironment().isResolveHostName()) {
            try {
                envTags.put("host", resolveHostName());
            } catch (UnknownHostException e) {
                if (log.isDebugEnabled()) {
                    log.debug("Failed to resolve host name.", e);
                }
            }
        }

        if (configuration.getTags().getProviders().getEnvironment().isResolveHostAddress()) {
            try {
                envTags.put("host-address", resolveHostAddress());
            } catch (UnknownHostException e) {
                if (log.isDebugEnabled()) {
                    log.debug("Failed to resolve host address.", e);
                }
            }
        }

        return envTags;
    }

    private static String resolveHostName() throws UnknownHostException {
        return InetAddress.getLocalHost().getHostName();
    }

    private static String resolveHostAddress() throws UnknownHostException {
        return InetAddress.getLocalHost().getHostAddress();
    }

}
