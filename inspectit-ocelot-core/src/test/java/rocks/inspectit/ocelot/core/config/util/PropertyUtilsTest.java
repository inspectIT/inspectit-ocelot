package rocks.inspectit.ocelot.core.config.util;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class PropertyUtilsTest {

    @Test
    public void testJsonParsing() throws Exception {
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
