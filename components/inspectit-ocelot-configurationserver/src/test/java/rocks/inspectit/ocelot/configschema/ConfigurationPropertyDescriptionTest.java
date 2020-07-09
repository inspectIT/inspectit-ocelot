package rocks.inspectit.ocelot.configschema;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigurationPropertyDescriptionTest {

    @Nested
    class Constructor {

        @Test
        void verifyCompositeChildrenSorted() {
            ConfigurationPropertyDescription compA = ConfigurationPropertyDescription.builder()
                    .type(ConfigurationPropertyType.COMPOSITE)
                    .propertyName("A")
                    .build();
            ConfigurationPropertyDescription flatB = ConfigurationPropertyDescription.builder()
                    .type(ConfigurationPropertyType.STRING)
                    .propertyName("B")
                    .build();
            ConfigurationPropertyDescription compC = ConfigurationPropertyDescription.builder()
                    .type(ConfigurationPropertyType.COMPOSITE)
                    .propertyName("C")
                    .build();
            ConfigurationPropertyDescription flatD = ConfigurationPropertyDescription.builder()
                    .type(ConfigurationPropertyType.ENUM)
                    .propertyName("D")
                    .build();

            ConfigurationPropertyDescription result = ConfigurationPropertyDescription.builder()
                    .type(ConfigurationPropertyType.COMPOSITE)
                    .child(flatD)
                    .child(compA)
                    .child(flatB)
                    .child(compC)
                    .build();

            assertThat(result.getChildren()).containsExactly(flatB, flatD, compA, compC);
        }

        @Test
        void verifyEnumValuesSorted() {
            ConfigurationPropertyDescription enumProp = ConfigurationPropertyDescription.builder()
                    .type(ConfigurationPropertyType.ENUM)
                    .enumValue("B")
                    .enumValue("C")
                    .enumValue("A")
                    .build();

            assertThat(enumProp.getEnumValues()).containsExactly("A", "B", "C");
        }

    }
}
