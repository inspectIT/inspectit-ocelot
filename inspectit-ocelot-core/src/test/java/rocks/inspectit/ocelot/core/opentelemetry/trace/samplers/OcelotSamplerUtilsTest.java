package rocks.inspectit.ocelot.core.opentelemetry.trace.samplers;

import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.config.model.tracing.SampleMode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test class for {@link OcelotSamplerUtils}
 */
public class OcelotSamplerUtilsTest {

    private final Sampler traceIdRatioBasedSampler = Sampler.traceIdRatioBased(0.5);

    private final Sampler parentBasedSampler = Sampler.parentBased(Sampler.traceIdRatioBased(0.75));

    private final Sampler hybridSampler = HybridParentTraceIdRatioBasedSampler.create(1.0);

    @Test
    void testCreateParentBased() {
        Sampler sampler = OcelotSamplerUtils.create(SampleMode.PARENT_BASED, .75);
        assertThat(sampler).isEqualTo(parentBasedSampler);
    }

    @Test
    void testCreateTraceIdRatioBased() {
        Sampler sampler = OcelotSamplerUtils.create(SampleMode.TRACE_ID_RATIO_BASED, .5);
        assertThat(sampler).isEqualTo(traceIdRatioBasedSampler);
    }

    @Test
    void testCreateHybrid() {
        Sampler sampler = OcelotSamplerUtils.create(SampleMode.HYBRID_PARENT_TRACE_ID_RATIO_BASED, 1);
        assertThat(sampler).isEqualTo(hybridSampler);
    }

    @Test
    void testExtractSampleProbability() {
        assertThat(OcelotSamplerUtils.extractSampleProbability(traceIdRatioBasedSampler)).isEqualTo(0.5);

        assertThat(OcelotSamplerUtils.extractSampleProbability(hybridSampler)).isEqualTo(1);

        assertThrows(RuntimeException.class, () -> OcelotSamplerUtils.extractSampleProbability(Sampler.alwaysOn()));
    }

    @Test
    void testExtractSampleMode() {
        assertThat(OcelotSamplerUtils.extractSampleMode(parentBasedSampler)).isEqualTo(SampleMode.PARENT_BASED);
        assertThat(OcelotSamplerUtils.extractSampleMode(traceIdRatioBasedSampler)).isEqualTo(SampleMode.TRACE_ID_RATIO_BASED);
        assertThat(OcelotSamplerUtils.extractSampleMode(hybridSampler)).isEqualTo(SampleMode.HYBRID_PARENT_TRACE_ID_RATIO_BASED);
        assertThrows(RuntimeException.class, () -> OcelotSamplerUtils.extractSampleMode(Sampler.alwaysOn()));
    }
}
