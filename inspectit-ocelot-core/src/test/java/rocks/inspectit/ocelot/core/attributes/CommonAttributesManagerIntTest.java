package rocks.inspectit.ocelot.core.attributes;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.context.Scope;
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

        @Test
        public void baggageAvailable() {
            Baggage commonBaggage = provider.getCommonBaggage();

            assertThat(commonBaggage.isEmpty()).isFalse();
        }

        @Test
        public void attributeKeysCorrect() {
            Baggage commonBaggage = provider.getCommonBaggage();
            List<String> commonAttributeKeys = provider.getCommonAttributeKeys();

            assertThat(commonBaggage.asMap())
                    .allSatisfy((key, valueEntry) -> assertThat(commonAttributeKeys.contains(key)).isTrue());
        }

        @Test
        public void scopeAvailable() {
            Scope scope = provider.withCommonAttributesScope();
            assertThat(scope).isNotNull();
            scope.close();
        }
    }

    @Nested
    @DirtiesContext
    @TestPropertySource(properties = {
            "inspectit.attributes.extra.service-name=my-service-name"
    })
    class PriorityRespected extends SpringTestBase {

        @Autowired
        CommonAttributesManager provider;

        @Test
        public void extraOverwritesProviders() {
            Baggage commonBaggage = provider.getCommonBaggage();

            assertThat(commonBaggage.asMap())
                    .anySatisfy((key, valueEntry) -> {
                        assertThat(key).isEqualTo("service-name");
                        assertThat(valueEntry.getValue()).isEqualTo("my-service-name");
                    });
        }
    }

    @Nested
    @DirtiesContext
    class Updates extends SpringTestBase {

        @Autowired
        CommonAttributesManager provider;

        @Test
        public void extraOverwritesProviders() {
            updateProperties(
                    properties -> properties
                            .withProperty("inspectit.attributes.providers.environment.resolve-host-address", Boolean.FALSE)
                            .withProperty("inspectit.attributes.providers.environment.resolve-host-name", Boolean.FALSE)
                            .withProperty("inspectit.service-name", "some-service-name")
                            .withProperty("inspectit.attributes.extra.service-name", "my-service-name")
            );

            Baggage commonBaggage = provider.getCommonBaggage();

            assertThat(commonBaggage.asMap())
                    .anySatisfy((key, valueEntry) -> {
                        assertThat(key).isEqualTo("service-name");
                        assertThat(valueEntry.getValue()).isEqualTo("my-service-name");
                    })
                    .allSatisfy((key, valueEntry) ->
                        assertThat(key).isNotIn("host", "host-address")
                    );
        }
    }

    @Nested
    @DirtiesContext
    @TestPropertySource(properties = {
            "inspectit.attributes.extra.service-name=this-value-is-over-255-characters-long ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------",
            "inspectit.attributes.extra.service-name2=non-printable-character-\u007f"
    })
    class VeryLongAttributeValues extends SpringTestBase {

        @Autowired
        CommonAttributesManager provider;

        @Test
        public void extraOverwritesProviders() {
            Baggage commonBaggage = provider.getCommonBaggage();

            assertThat(commonBaggage.asMap())
                    .anySatisfy((key, valueEntry) -> {
                        assertThat(key).isEqualTo("service-name");
                        assertThat(valueEntry.getValue()).isEqualTo("<invalid>");
                    })
                    .anySatisfy((key, valueEntry) -> {
                        assertThat(key).isEqualTo("service-name2");
                        assertThat(valueEntry.getValue()).isEqualTo("<invalid>");
                    });
        }
    }
}
