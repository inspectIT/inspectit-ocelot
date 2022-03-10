package rocks.inspectit.ocelot.config.model.exporters;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class for {@link rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledStatep}
 */
public class ExporterEnabledStateTest {

    @Test
    public void testDisabled(){

        // test that isDisabled() is only true for DISABLED
        assertThat(ExporterEnabledState.DISABLED.isDisabled());

        assertThat(!ExporterEnabledState.IF_CONFIGURED.isDisabled());

        assertThat(!ExporterEnabledState.ENABLED.isDisabled());
    }
}
