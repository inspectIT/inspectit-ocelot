package rocks.inspectit.ocelot.core.tags;

import io.opencensus.tags.InternalUtils;
import io.opencensus.tags.TagContext;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.TagValue;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import rocks.inspectit.ocelot.core.SpringTestBase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CommonTagsManagerIntTest {

    @Nested
    @DirtiesContext
    class Defaults extends SpringTestBase {

        @Autowired
        CommonTagsManager provider;

        public void contextAvailable() {
            TagContext commonTagContext = provider.getCommonTagContext();

            assertThat(InternalUtils.getTags(commonTagContext)).isNotEmpty();
        }

        public void tagKeysCorrect() {
            TagContext commonTagContext = provider.getCommonTagContext();
            List<TagKey> commonTagKeys = provider.getCommonTagKeys();

            assertThat(InternalUtils.getTags(commonTagContext))
                    .allSatisfy(tag -> assertThat(commonTagKeys.contains(tag.getKey())).isTrue());
        }

        public void scopeAvailable() {
            assertThat(provider.withCommonTagScope()).isNotNull();
        }
    }


    @Nested
    @DirtiesContext
    @TestPropertySource(properties = {
            "inspectit.tags.extra.service-name=my-service-name"
    })
    class PriorityRespected extends SpringTestBase {

        @Autowired
        CommonTagsManager provider;

        @Test
        public void extraOverwritesProviders() {
            TagContext commonTagContext = provider.getCommonTagContext();

            assertThat(InternalUtils.getTags(commonTagContext))
                    .anySatisfy(tag -> {
                        assertThat(tag.getKey()).isEqualTo(TagKey.create("service-name"));
                        assertThat(tag.getValue()).isEqualTo(TagValue.create("my-service-name"));
                    });
        }
    }

    @Nested
    @DirtiesContext
    @TestPropertySource(properties = {
            "inspectit.tags.extra.service-name=my-service-name"
    })
    class Updates extends SpringTestBase {

        @Autowired
        CommonTagsManager provider;

        @Test
        public void extraOverwritesProviders() {
            updateProperties(
                    properties -> properties
                            .withProperty("inspectit.tags.providers.environment.resolve-host-address", Boolean.FALSE)
                            .withProperty("inspectit.tags.providers.environment.resolve-host-name", Boolean.FALSE)
                            .withProperty("inspectit.service-name", "some-service-name")
                            .withProperty("inspectit.tags.extra.service-name", "my-service-name")
            );

            TagContext commonTagContext = provider.getCommonTagContext();

            assertThat(InternalUtils.getTags(commonTagContext))
                    .anySatisfy(tag -> {
                        assertThat(tag.getKey()).isEqualTo(TagKey.create("service-name"));
                        assertThat(tag.getValue()).isEqualTo(TagValue.create("my-service-name"));
                    })
                    .allSatisfy(tag -> {
                        assertThat(tag.getKey()).isNotIn("host", "host-address");
                    });
        }
    }

    @Nested
    @DirtiesContext
    @TestPropertySource(properties = {
            "inspectit.tags.extra.service-name=this-value-is-over-255-characters-long ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------"
    })
    class VeryLongTagValues extends SpringTestBase {

        @Autowired
        CommonTagsManager provider;

        @Test
        public void extraOverwritesProviders() {
            TagContext commonTagContext = provider.getCommonTagContext();

            assertThat(InternalUtils.getTags(commonTagContext))
                    .anySatisfy(tag -> {
                        assertThat(tag.getKey()).isEqualTo(TagKey.create("service-name"));
                        assertThat(tag.getValue()).isEqualTo(TagValue.create("<invalid>"));
                    });
        }
    }

}