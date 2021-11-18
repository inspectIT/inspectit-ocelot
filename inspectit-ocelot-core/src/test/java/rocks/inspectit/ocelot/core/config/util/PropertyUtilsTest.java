package rocks.inspectit.ocelot.core.config.util;

import com.fasterxml.jackson.core.JsonParseException;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.core.config.propertysources.http.RawProperties;

import java.io.IOException;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class PropertyUtilsTest {

    @Nested
    public class ReadJson {

        @Test
        public void readJson() throws Exception {
            String json = "{arr: [{x:42},{y:7},[\"A\",\"B\"]],nested:{name:\"blub\",\"complex.key\":true}}";

            Properties result = PropertyUtils.readJson(json);

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
            String json = "root:\n  sub-child:\n    value: true\n  second: 42";

            Properties result = PropertyUtils.readYaml(json);

            assertThat(result).hasSize(2);
            assertThat(result).containsEntry("root.sub-child.value", true);
            assertThat(result).containsEntry("root.second", 42);

        }
    }

    @Nested
    class Read {

        String json = "{\"arr\": [{\"x\":42},{\"y\":7},[\"A\",\"B\"]],nested:{\"name\":\"blub\",\"complex.key\":true}}";

        String yaml = "root:\n  sub-child:\n    value: true\n  second: 42";

        @Test
        public void read() throws IOException {

            RawProperties confJ = new RawProperties(json, ContentType.APPLICATION_JSON.getMimeType());

            // test that json is correctly parsed
            Properties resultJ1 = PropertyUtils.read(confJ);
            assertThat(resultJ1).hasSize(6);
            assertThat(resultJ1).containsEntry("arr[0].x", 42);
            Properties resultJ2 = PropertyUtils.read(json, ContentType.APPLICATION_JSON.getMimeType());
            assertThat(resultJ1).isEqualTo(resultJ2);

            // test that yaml is correctly parsed
            RawProperties confY = new RawProperties(yaml, "application/x-yaml");
            Properties resultY = PropertyUtils.read(confY);
            assertThat(resultY).hasSize(2);
            assertThat(resultY).containsEntry("root.sub-child.value", true);
            assertThat(resultY).isEqualTo(PropertyUtils.read(yaml, "application/x-yaml"));

            // test MIME type mismatch
            Assertions.assertThrows(JsonParseException.class, () -> PropertyUtils.read(yaml, ContentType.APPLICATION_JSON.getMimeType()));

            // TODO: YamlPropertiesFactoryBean can also parse JSON, although only quoted keys are supported.
            Properties resultN = PropertyUtils.read(json, "application/x-yaml");
            assertThat(resultJ1).isEqualTo(resultN);

        }

    }
}
