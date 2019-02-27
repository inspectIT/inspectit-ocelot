package rocks.inspectit.oce.core.instrumentation.config;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DataProviderCallSchedulerTest {

    private DataProviderCallScheduler scheduler = new DataProviderCallScheduler();

    @Nested
    class GetInTopologicalOrder {

        @Test
        void testElementsPreserved() throws Exception {
            Map<String, Set<String>> dependencies = new HashMap<>();
            dependencies.put("A", Sets.newHashSet("B"));
            dependencies.put("B", Sets.newHashSet());
            dependencies.put("C", Sets.newHashSet());
            List<String> result = scheduler.getInTopologicalOrder(dependencies);

            assertThat(result).containsExactlyInAnyOrder("A", "B", "C");
        }

        @Test
        void testTransitiveOrdering() throws Exception {
            Map<String, Set<String>> dependencies = new HashMap<>();
            dependencies.put("A", Sets.newHashSet("B"));
            dependencies.put("B", Sets.newHashSet("C"));
            dependencies.put("C", Sets.newHashSet("D"));
            dependencies.put("D", Sets.newHashSet());
            List<String> result = scheduler.getInTopologicalOrder(dependencies);

            assertThat(result).containsExactly("D", "C", "B", "A");
        }

        @Test
        void testDAGOrdering() throws Exception {
            Map<String, Set<String>> dependencies = new HashMap<>();
            dependencies.put("A", Sets.newHashSet("B", "C"));
            dependencies.put("B", Sets.newHashSet("E"));
            dependencies.put("C", Sets.newHashSet("D"));
            dependencies.put("D", Sets.newHashSet("E"));
            dependencies.put("E", Sets.newHashSet());
            List<String> result = scheduler.getInTopologicalOrder(dependencies);

            assertThat(result).containsExactlyInAnyOrder("A", "B", "C", "D", "E");
            assertThat(result).containsSubsequence("B", "A");
            assertThat(result).containsSubsequence("C", "A");
            assertThat(result).containsSubsequence("E", "B");
            assertThat(result).containsSubsequence("D", "C");
            assertThat(result).containsSubsequence("E", "D");
        }

        @Test
        void testDirectCycleDetected() {
            Map<String, Set<String>> dependencies = new HashMap<>();
            dependencies.put("A", Sets.newHashSet("B"));
            dependencies.put("B", Sets.newHashSet("A"));
            dependencies.put("C", Sets.newHashSet("A"));
            assertThatThrownBy(() -> scheduler.getInTopologicalOrder(dependencies))
                    .isInstanceOf(DataProviderCallScheduler.CyclicDataDependencyException.class);
        }

        @Test
        void testIndirectCycleDetected() {
            Map<String, Set<String>> dependencies = new HashMap<>();
            dependencies.put("A", Sets.newHashSet("B"));
            dependencies.put("B", Sets.newHashSet("C"));
            dependencies.put("C", Sets.newHashSet("A"));
            assertThatThrownBy(() -> scheduler.getInTopologicalOrder(dependencies))
                    .isInstanceOf(DataProviderCallScheduler.CyclicDataDependencyException.class);
        }

    }
}
