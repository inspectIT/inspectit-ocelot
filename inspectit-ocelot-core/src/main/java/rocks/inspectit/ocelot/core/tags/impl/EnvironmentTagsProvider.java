package rocks.inspectit.ocelot.core.tags.impl;

import io.opentelemetry.semconv.ServiceAttributes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.tags.providers.EnvironmentTagsProviderSettings;
import rocks.inspectit.ocelot.core.tags.ICommonTagsProvider;
import io.opentelemetry.sdk.resources.Resource;

import javax.validation.Valid;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link ICommonTagsProvider} that provides the environment based tags like service name, host, etc.
 * <p>
 * SIDE NOTE: Normally, {@code host.name} and {@code host.ip} would be attributes of our {@link Resource},
 * but since we also would like to allow filtering metrics with them (in InfluxDB), we use them as environment tags.
 */
@Component
@Slf4j
public class EnvironmentTagsProvider implements ICommonTagsProvider {

    @Override
    public Map<String, String> getTags(InspectitConfig configuration) {
        @Valid EnvironmentTagsProviderSettings conf = configuration.getTags().getProviders().getEnvironment();
        if (conf.isEnabled()) {
            Map<String, String> envTags = new HashMap<>();

            // DEPRECATED: To comply with OTel semantic conventions, we should replace the 'service' tag
            // with 'service.name'
            envTags.put("service", configuration.getServiceName());

            // We should use only this one in the future
            envTags.put(ServiceAttributes.SERVICE_NAME.getKey(), configuration.getServiceName());

            if (conf.isResolveHostName()) {
                try {
                    envTags.put("host.name", resolveHostName());
                } catch (UnknownHostException e) {
                    if (log.isDebugEnabled()) {
                        log.debug("Failed to resolve host name", e);
                    }
                }
            }

            if (conf.isResolveHostIp()) {
                try {
                    envTags.put("host.ip", resolveHostAddress());
                } catch (UnknownHostException e) {
                    if (log.isDebugEnabled()) {
                        log.debug("Failed to resolve host address", e);
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
