package rocks.inspectit.oce.core.utils;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class CommonUtilsTest {

    @Nested
    class LocateTypeWithinImports {

        @Test
        void checkPrimitivesFound() {
            assertThat(CommonUtils.locateTypeWithinImports("int", null, Collections.emptyList()))
                    .isSameAs(int.class);
        }

        @Test
        void checkJavaLangImported() {
            assertThat(CommonUtils.locateTypeWithinImports("String", getClass().getClassLoader(), Collections.emptyList()))
                    .isSameAs(String.class);
        }

        @Test
        void checkImportsWork() {
            assertThat(CommonUtils.locateTypeWithinImports("Duration", getClass().getClassLoader(), Collections.singletonList("java.time")))
                    .isSameAs(Duration.class);
        }

        @Test
        void checkFQNsWork() {
            assertThat(CommonUtils.locateTypeWithinImports("java.time.Duration", getClass().getClassLoader(), Collections.singletonList("something.else")))
                    .isSameAs(Duration.class);
        }
    }
}
