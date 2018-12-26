package rocks.inspectit.oce.core.tags.impl;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import rocks.inspectit.oce.core.SpringTestBase;

import static org.assertj.core.api.Assertions.assertThat;

class ExtrasTagsProviderIntTest {

    @Nested
    class Defaults extends SpringTestBase {

        @Autowired
        ExtrasTagsProvider provider;

        @Test
        public void happyPath() {
            assertThat(provider.isEnabled()).isFalse();
        }

    }

    @Nested
    @DirtiesContext
    @TestPropertySource(properties = {
            "inspectit.tags.extra.key1=value1",
            "inspectit.tags.extra[key2]=value2",
    })
    class Defined extends SpringTestBase {

        @Autowired
        ExtrasTagsProvider provider;

        @Test
        public void happyPath() {
            assertThat(provider.isEnabled()).isTrue();
            assertThat(provider.getTags())
                    .hasSize(2)
                    .containsEntry("key1", "value1")
                    .containsEntry("key2", "value2");
        }

    }

    @Nested
    @DirtiesContext
    @TestPropertySource(properties = {
            "inspectit.tags.extra.key1=value1",
            "inspectit.tags.extra[key2]=value2",
    })
    class Updated extends SpringTestBase {

        @Autowired
        ExtrasTagsProvider provider;

        @Test
        public void happyPath() {
            updateProperties(properties -> properties.withProperty("inspectit.tags.extra.key1", "updatedValue"));

            assertThat(provider.isEnabled()).isTrue();
            assertThat(provider.getTags())
                    .hasSize(2)
                    .containsEntry("key1", "updatedValue")
                    .containsEntry("key2", "value2");
        }

    }

}
