package rocks.inspectit.ocelot.core.tags.impl;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import rocks.inspectit.ocelot.core.SpringTestBase;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class ExtrasTagsProviderIntTest {

    @Nested
    class Defaults extends SpringTestBase {

        @Autowired
        InspectitEnvironment env;

        @Autowired
        ExtrasCommonTagsProvider provider;

        @Test
        public void happyPath() {
            assertThat(provider.getTags(env.getCurrentConfig())).isEmpty();
        }

    }

    @Nested
    @DirtiesContext
    @TestPropertySource(properties = {"inspectit.tags.extra.key1=value1", "inspectit.tags.extra[key2]=value2",})
    class Defined extends SpringTestBase {

        @Autowired
        InspectitEnvironment env;

        @Autowired
        ExtrasCommonTagsProvider provider;

        @Test
        public void happyPath() {
            assertThat(provider.getTags(env.getCurrentConfig())).hasSize(2)
                    .containsEntry("key1", "value1")
                    .containsEntry("key2", "value2");
        }

    }

    @Nested
    @DirtiesContext
    @TestPropertySource(properties = {"inspectit.tags.extra.key1=value1", "inspectit.tags.extra[key2]=value2",})
    class Updated extends SpringTestBase {

        @Autowired
        InspectitEnvironment env;

        @Autowired
        ExtrasCommonTagsProvider provider;

        @Test
        public void happyPath() {
            updateProperties(properties -> properties.withProperty("inspectit.tags.extra.key1", "updatedValue"));

            assertThat(provider.getTags(env.getCurrentConfig())).hasSize(2)
                    .containsEntry("key1", "updatedValue")
                    .containsEntry("key2", "value2");
        }

    }

}
