package rocks.inspectit.ocelot.core.attributes;

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

class CommonAttributesManagerIntTest {

    @Nested
    @DirtiesContext
    class Defaults extends SpringTestBase {

        @Autowired
        CommonAttributesManager provider;

        public void contextAvailable() {
            TagContext commonTagContext = provider.getCommonBaggage();

            assertThat(InternalUtils.getTags(commonTagContext)).toIterable().isNotEmpty();
        }

        public void tagKeysCorrect() {
            TagContext commonTagContext = provider.getCommonBaggage();
            List<TagKey> commonTagKeys = provider.getCommonAttributeKeys();

            assertThat(InternalUtils.getTags(commonTagContext))
                    .toIterable().allSatisfy(tag -> assertThat(commonTagKeys.contains(tag.getKey())).isTrue());
        }

        public void scopeAvailable() {
            assertThat(provider.withCommonAttributesScope()).isNotNull();
        }
    }


    @Nested
    @DirtiesContext
    @TestPropertySource(properties = {
            "inspectit.tags.extra.service-name=my-service-name"
    })
    class PriorityRespected extends SpringTestBase {

        @Autowired
        CommonAttributesManager provider;

        @Test
        public void extraOverwritesProviders() {
            TagContext commonTagContext = provider.getCommonBaggage();

            assertThat(InternalUtils.getTags(commonTagContext))
                    .toIterable().anySatisfy(tag -> {
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
        CommonAttributesManager provider;

        @Test
        public void extraOverwritesProviders() {
            updateProperties(
                    properties -> properties
                            .withProperty("inspectit.tags.providers.environment.resolve-host-address", Boolean.FALSE)
                            .withProperty("inspectit.tags.providers.environment.resolve-host-name", Boolean.FALSE)
                            .withProperty("inspectit.service-name", "some-service-name")
                            .withProperty("inspectit.tags.extra.service-name", "my-service-name")
            );

            TagContext commonTagContext = provider.getCommonBaggage();

            assertThat(InternalUtils.getTags(commonTagContext))
                    .toIterable().anySatisfy(tag -> {
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
            "inspectit.tags.extra.service-name=this-value-is-over-255-characters-long ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------",
            "inspectit.tags.extra.service-name2=non-printable-character-\u007f"
    })
    class VeryLongTagValues extends SpringTestBase {

        @Autowired
        CommonAttributesManager provider;

        @Test
        public void extraOverwritesProviders() {
            TagContext commonTagContext = provider.getCommonBaggage();

            assertThat(InternalUtils.getTags(commonTagContext))
                    .toIterable().anySatisfy(tag -> {
                        assertThat(tag.getKey()).isEqualTo(TagKey.create("service-name"));
                        assertThat(tag.getValue()).isEqualTo(TagValue.create("<invalid>"));
                    });
        }
    }

}
