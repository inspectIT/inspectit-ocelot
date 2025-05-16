package rocks.inspectit.ocelot.core.opentelemetry.resource;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.instrumentation.resources.*;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.semconv.TelemetryAttributes;
import lombok.Getter;
import rocks.inspectit.ocelot.bootstrap.AgentManager;

/**
 * Provides static resource attributes, which will not change during runtime. <br>
 * There should be a list of all available OpenTelemetry Resource Providers here:
 * <a href="https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/resources/library">
 */
public class ResourceAttributesProvider {

    @Getter
    private final static Attributes tracerProviderResourceAttributes = createTracerProviderResourceAttributes();

    /**
     * First, we create inspectIT specific attributes by ourselves.
     * Then, we use the OpenTelemetry {@code ResourceProvider} to create additional resources.
     *
     * @return the static resource attributes for the {@link TracerProvider}
     */
    private static Attributes createTracerProviderResourceAttributes() {
        AttributesBuilder builder = Attributes.builder();

        builder.put(AttributeKey.stringKey("inspectit.agent.version"), AgentManager.getAgentVersion());
        builder.put(TelemetryAttributes.TELEMETRY_SDK_VERSION, AgentManager.getOpenTelemetryVersion());
        builder.put(TelemetryAttributes.TELEMETRY_SDK_LANGUAGE, "java");
        builder.put(TelemetryAttributes.TELEMETRY_SDK_NAME, "opentelemetry");

        // we already use host.name as environment tag, see EnvironmentTagsProvider
        AttributeKey<String> hostArchKey = AttributeKey.stringKey("host.arch");
        builder.put(hostArchKey, HostResource.get().getAttribute(hostArchKey));
        builder.putAll(HostIdResource.get().getAttributes());
        builder.putAll(OsResource.get().getAttributes());
        builder.putAll(ProcessResource.get().getAttributes());
        builder.putAll(ProcessRuntimeResource.get().getAttributes());
        builder.putAll(ContainerResource.get().getAttributes());

        return builder.build();
    }
}
