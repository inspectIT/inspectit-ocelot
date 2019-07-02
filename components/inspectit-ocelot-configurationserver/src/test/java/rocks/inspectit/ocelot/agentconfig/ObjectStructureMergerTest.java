package rocks.inspectit.ocelot.agentconfig;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ObjectStructureMergerTest {

    @Nested
    class Merge {

        @Test
        void verifyMergePriority() {
            Object a = map("key", "value");
            Object b = list("list");

            assertThat(ObjectStructureMerger.merge(a, b)).isSameAs(a);
        }


        @Test
        void mergerShorterWithLongerList() {
            Object a = list("a", "b");
            Object b = list("z", "z", "c");

            assertThat(ObjectStructureMerger.merge(a, b))
                    .isEqualTo(list("a", "b", "c"));
        }

        @Test
        void mergerLongerWithShorterList() {
            Object a = list("a", "b", "c");
            Object b = list("z", "z");

            assertThat(ObjectStructureMerger.merge(a, b))
                    .isEqualTo(list("a", "b", "c"));
        }

        @Test
        void mergeComplex() {

            Object a = map(
                    "uniqueA", "A1",
                    "sharedVal", "A2",
                    "sharedMap", map(
                            "A", "A"
                    ),
                    "sharedList", list("A")
            );

            Object b = map(
                    "uniqueB", "B1",
                    "sharedVal", "B2",
                    "sharedMap", map(
                            "B", "B"
                    ),
                    "sharedList", list("C", "B")
            );

            Object merged = ObjectStructureMerger.merge(a, b);

            assertThat(merged).isEqualTo(
                    map(
                            "uniqueA", "A1",
                            "sharedVal", "A2",
                            "sharedMap", map(
                                    "A", "A",
                                    "B", "B"
                            ),
                            "sharedList", list("A", "B"),
                            "uniqueB", "B1"
                    )
            );
        }

    }

    private static Map<Object, Object> map(Object... keysAndValues) {
        LinkedHashMap<Object, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            result.put(keysAndValues[i], keysAndValues[i + 1]);
        }
        return result;
    }

    private static List<Object> list(Object... elements) {
        return Arrays.asList(elements);
    }
}
