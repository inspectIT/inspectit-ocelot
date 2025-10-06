package rocks.inspectit.ocelot.core.opentelemetry.resource;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.instrumentation.resources.*;
import io.opentelemetry.semconv.TelemetryAttributes;
import rocks.inspectit.ocelot.bootstrap.AgentManager;

/**
 * Provides static resource attributes, which will not change during runtime. <br>
 * There should be a list of all available OpenTelemetry Resource Providers here:
 * <a href="https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/resources/library">
 */
public class ResourceAttributesProvider {

    private static Attributes tracerProviderResourceAttributes;

    private static Attributes meterProviderResourceAttributes;

    /**
     * @return the resource attributes for traces
     */
    public static Attributes getTracerProviderResourceAttributes() {
        if (tracerProviderResourceAttributes == null)
            tracerProviderResourceAttributes = createTracerProviderResourceAttributes();
        return tracerProviderResourceAttributes;
    }

    /**
     * @return the resource attributes for metrics
     */
    public static Attributes getMeterProviderResourceAttributes() {
        if (meterProviderResourceAttributes == null)
            meterProviderResourceAttributes = createMeterProviderResourceAttributes();
        return meterProviderResourceAttributes;
    }

    /**
     * First, we create inspectIT specific attributes by ourselves.
     * Then, we use the OpenTelemetry {@code ResourceProvider} to create additional resources.
     *
     * @return the resource attributes for traces
     */
    private static Attributes createTracerProviderResourceAttributes() {
        AttributesBuilder builder = getDefaultResourceAttributesBuilder();

        builder.put(AttributeKey.stringKey("inspectit.agent.version"), AgentManager.getAgentVersion());

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

    /**
     * @return the resource attributes for metrics
     */
    private static Attributes createMeterProviderResourceAttributes() {
        AttributesBuilder builder = getDefaultResourceAttributesBuilder();
        return builder.build();
    }

    /**
     * @return the default resource attributes for all signals
     */
    private static AttributesBuilder getDefaultResourceAttributesBuilder() {
        AttributesBuilder builder = Attributes.builder();

        builder.put(TelemetryAttributes.TELEMETRY_SDK_VERSION, AgentManager.getOpenTelemetryVersion());
        builder.put(TelemetryAttributes.TELEMETRY_SDK_LANGUAGE, "java");
        builder.put(TelemetryAttributes.TELEMETRY_SDK_NAME, "opentelemetry");

        return builder;
    }
}
