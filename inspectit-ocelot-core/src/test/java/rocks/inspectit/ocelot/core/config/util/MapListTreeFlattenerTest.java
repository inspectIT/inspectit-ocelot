package rocks.inspectit.ocelot.core.config.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MapListTreeFlattenerTest {

    @Test
    public void testListFlattening() {

        Map<String, Object> root = new HashMap<>();
        root.put("l1", Arrays.asList("A", "B", "C"));
        root.put("l2", Arrays.asList(42));

        Map<String, Object> result = MapListTreeFlattener.flatten(root);
        assertThat(result).hasSize(4);
        assertThat(result).containsEntry("l1[0]", "A");
        assertThat(result).containsEntry("l1[1]", "B");
        assertThat(result).containsEntry("l1[2]", "C");
        assertThat(result).containsEntry("l2[0]", 42);

    }

    @Test
    public void testComplexKeys() {

        Map<String, Object> root = new HashMap<>();
        Map<String, Object> nested = new HashMap<>();
        root.put("nested", nested);

        nested.put("key", "nestedValue");
        nested.put("complex.key", 42);

        Map<String, Object> result = MapListTreeFlattener.flatten(root);
        assertThat(result).hasSize(2);
        assertThat(result).containsEntry("nested.key", "nestedValue");
        assertThat(result).containsEntry("nested.complex.key", 42);

    }

    @Test
    public void testInvalidType() {
        assertThatThrownBy(() -> MapListTreeFlattener.flatten(new HashSet<>())).isInstanceOf(RuntimeException.class);
    }

    @Test
    public void testNestingInArray() {

        Map<String, Object> objA = new HashMap<>();
        objA.put("name", "Hans");
        Map<String, Object> objB = new HashMap<>();
        objB.put("name", "Fritz");

        Map<String, Object> result = MapListTreeFlattener.flatten(Arrays.asList(objA, objB));
        assertThat(result).hasSize(2);
        assertThat(result).containsEntry("[0].name", "Hans");
        assertThat(result).containsEntry("[1].name", "Fritz");

    }

}
