package rocks.inspectit.ocelot.core.config.util;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class PropertyUtilsTest {

    String json = "{\"arr\": [{\"x\":42},{\"y\":7},[\"A\",\"B\"]],nested:{\"name\":\"blub\",\"complex.key\":true}}";

    String yaml = "root:\n  sub-child:\n    value: true\n  second: 42";

    @Nested
    public class ReadJson {

        @Test
        public void readJson() {

            Properties result = PropertyUtils.readYamlOrJson(json);

            assertThat(result).hasSize(6);
            assertThat(result).containsEntry("arr[0].x", 42);
            assertThat(result).containsEntry("arr[1].y", 7);
            assertThat(result).containsEntry("arr[2][0]", "A");
            assertThat(result).containsEntry("arr[2][1]", "B");
            assertThat(result).containsEntry("nested.name", "blub");
            assertThat(result).containsEntry("nested.complex.key", true);

        }
    }

    @Nested
    public class ReadYaml {

        @Test
        public void readYaml() {

            Properties result = PropertyUtils.readYamlOrJson(yaml);

            assertThat(result).hasSize(2);
            assertThat(result).containsEntry("root.sub-child.value", true);
            assertThat(result).containsEntry("root.second", 42);

        }

    }
}