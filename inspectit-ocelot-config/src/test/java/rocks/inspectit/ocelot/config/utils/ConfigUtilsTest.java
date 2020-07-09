package rocks.inspectit.ocelot.config.utils;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ConfigUtilsTest {

    @Nested
    class LocateTypeWithinImports {

        @Test
        void checkPrimitivesFound() {
            assertThat(ConfigUtils.locateTypeWithinImports("int", null, Collections.emptyList())).isSameAs(int.class);
        }

        @Test
        void checkJavaLangImported() {
            assertThat(ConfigUtils.locateTypeWithinImports("String", getClass().getClassLoader(), Collections.emptyList()))
                    .isSameAs(String.class);
        }

        @Test
        void checkImportsWork() {
            assertThat(ConfigUtils.locateTypeWithinImports("Duration", getClass().getClassLoader(), Collections.singletonList("java.time")))
                    .isSameAs(Duration.class);
        }

        @Test
        void checkFQNsWork() {
            assertThat(ConfigUtils.locateTypeWithinImports("java.time.Duration", getClass().getClassLoader(), Collections
                    .singletonList("something.else"))).isSameAs(Duration.class);
        }
    }
}
