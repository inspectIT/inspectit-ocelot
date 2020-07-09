package rocks.inspectit.ocelot.core.tags.impl;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import rocks.inspectit.ocelot.core.SpringTestBase;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class EnvironmentTagsProviderIntTest {

    @Nested
    @DirtiesContext
    @TestPropertySource(properties = {"inspectit.service-name=" + Defaults.SERVICE_NAME})
    class Defaults extends SpringTestBase {

        static final String SERVICE_NAME = "SERVICE_NAME";

        @Autowired
        EnvironmentTagsProvider provider;

        @Autowired
        InspectitEnvironment env;

        @Test
        public void happyPath() {
            assertThat(provider.getTags(env.getCurrentConfig())).hasSize(3)
                    .containsEntry("service", SERVICE_NAME)
                    .containsKey("host")
                    .containsKey("host_address");
        }

    }

    @Nested
    @DirtiesContext
    @TestPropertySource(properties = {"inspectit.tags.providers.environment.resolve-host-name=false", "inspectit.tags.providers.environment.resolve-host-address=false",})
    class Overwritten extends SpringTestBase {

        @Autowired
        InspectitEnvironment env;

        @Autowired
        EnvironmentTagsProvider provider;

        @Test
        public void happyPath() {
            assertThat(provider.getTags(env.getCurrentConfig())).hasSize(1).containsKeys("service");
        }

    }

    @Nested
    @DirtiesContext
    @TestPropertySource(properties = {"inspectit.tags.providers.environment.enabled=false"})
    class Disabled extends SpringTestBase {

        @Autowired
        InspectitEnvironment env;

        @Autowired
        EnvironmentTagsProvider provider;

        @Test
        public void happyPath() {
            assertThat(provider.getTags(env.getCurrentConfig())).isEmpty();
        }

    }

    @Nested
    @DirtiesContext
    @TestPropertySource(properties = {"inspectit.tags.providers.environment.enabled=false"})
    class Update extends SpringTestBase {

        @Autowired
        EnvironmentTagsProvider provider;

        @Autowired
        InspectitEnvironment env;

        @Test
        public void enable() {
            updateProperties(properties -> properties.withProperty("inspectit.tags.providers.environment.enabled", Boolean.TRUE));

            assertThat(provider.getTags(env.getCurrentConfig())).hasSize(3);
        }

    }

    @Nested
    @DirtiesContext
    class UpdateServiceName extends SpringTestBase {

        @Autowired
        InspectitEnvironment env;

        @Autowired
        EnvironmentTagsProvider provider;

        @Test
        public void happyPath() {
            updateProperties(properties -> properties.withProperty("inspectit.service-name", "updatedName"));

            assertThat(provider.getTags(env.getCurrentConfig())).hasSize(3).containsEntry("service", "updatedName");
        }

    }

}
