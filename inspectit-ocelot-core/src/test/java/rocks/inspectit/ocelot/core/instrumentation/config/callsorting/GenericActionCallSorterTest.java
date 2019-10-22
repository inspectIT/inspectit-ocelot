package rocks.inspectit.ocelot.core.instrumentation.config.callsorting;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.config.model.instrumentation.actions.ActionCallSettings;
import rocks.inspectit.ocelot.core.instrumentation.config.model.ActionCallConfig;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class GenericActionCallSorterTest {

    private GenericActionCallSorter scheduler = new GenericActionCallSorter();


    private static class TestCallBuilder {

        private int counter = 0;

        private String name;
        private ActionCallSettings settings = new ActionCallSettings();

        TestCallBuilder(String name) {
            this.name = name;
        }

        ActionCallConfig build() {
            return ActionCallConfig.builder()
                    .name(name)
                    .callSettings(settings)
                    .build();
        }

        TestCallBuilder withDataInput(String data) {
            Map<String, String> dataInput = new HashMap<>(settings.getDataInput());
            dataInput.put("d_" + (counter++), data);
            settings.setDataInput(dataInput);
            return this;
        }

        TestCallBuilder withRead(String data) {
            Map<String, Boolean> read = new HashMap<>(settings.getOrder().getReads());
            read.put(data, true);
            settings.getOrder().setReads(read);
            return this;
        }

        TestCallBuilder withRemovedRead(String data) {
            Map<String, Boolean> read = new HashMap<>(settings.getOrder().getReads());
            read.put(data, false);
            settings.getOrder().setReads(read);
            return this;
        }

        TestCallBuilder withWrite(String data) {
            Map<String, Boolean> writes = new HashMap<>(settings.getOrder().getWrites());
            writes.put(data, true);
            settings.getOrder().setWrites(writes);
            return this;
        }

        TestCallBuilder withRemovedWrite(String data) {
            Map<String, Boolean> writes = new HashMap<>(settings.getOrder().getWrites());
            writes.put(data, false);
            settings.getOrder().setWrites(writes);
            return this;
        }

        TestCallBuilder withReadsBeforeWritten(String data) {
            Map<String, Boolean> readsBeforeWritten = new HashMap<>(settings.getOrder().getReadsBeforeWritten());
            readsBeforeWritten.put(data, true);
            settings.getOrder().setReadsBeforeWritten(readsBeforeWritten);
            return this;
        }

        TestCallBuilder onlyIfTrue(String data) {
            settings.setOnlyIfTrue(data);
            return this;
        }

        TestCallBuilder onlyIfFalse(String data) {
            settings.setOnlyIfFalse(data);
            return this;
        }

        TestCallBuilder onlyIfNull(String data) {
            settings.setOnlyIfNull(data);
            return this;
        }

        TestCallBuilder onlyIfNotNull(String data) {
            settings.setOnlyIfNotNull(data);
            return this;
        }
    }

    List<String> getNames(List<ActionCallConfig> calls) {
        return calls.stream()
                .map(ActionCallConfig::getName)
                .collect(Collectors.toList());
    }

    @Nested
    class OrderActionCalls {

        @Test
        void testElementsWithoutDependenciesPreserved() throws Exception {
            List<ActionCallConfig> input = Arrays.asList(
                    new TestCallBuilder("B").withDataInput("C").build(),
                    new TestCallBuilder("C").build(),
                    new TestCallBuilder("A").build()
            );

            List<String> result = getNames(scheduler.orderActionCalls(input));

            assertThat(result).containsExactly("A", "C", "B");
        }


        @Test
        void testTransitiveOrdering() throws Exception {
            List<ActionCallConfig> input = Arrays.asList(
                    new TestCallBuilder("A").withDataInput("B").build(),
                    new TestCallBuilder("B").withRead("C").build(),
                    new TestCallBuilder("C").onlyIfTrue("D").build(),
                    new TestCallBuilder("D").build()
            );

            List<String> result = getNames(scheduler.orderActionCalls(input));

            assertThat(result).containsExactly("D", "C", "B", "A");
        }

        @Test
        void testIdenticalActions() throws Exception {
            List<ActionCallConfig> input = Arrays.asList(
                    new TestCallBuilder("A").build(),
                    new TestCallBuilder("A").build()
            );

            List<String> result = getNames(scheduler.orderActionCalls(input));

            assertThat(result).containsExactly("A", "A");
        }

        @Test
        void testDAGOrdering() throws Exception {

            List<ActionCallConfig> input = Arrays.asList(
                    new TestCallBuilder("A").onlyIfNull("B").onlyIfNotNull("C").build(),
                    new TestCallBuilder("B").onlyIfFalse("E").build(),
                    new TestCallBuilder("C").withRead("D").build(),
                    new TestCallBuilder("D").build(),
                    new TestCallBuilder("E").withReadsBeforeWritten("D").build() //same as saying "D" depends on "E"
            );

            List<String> result = getNames(scheduler.orderActionCalls(input));

            assertThat(result).containsExactly("E", "B", "D", "C", "A");
        }


        @Test
        void testDirectCycleDetected() {

            List<ActionCallConfig> input = Arrays.asList(
                    new TestCallBuilder("A").withRead("B").build(),
                    new TestCallBuilder("B").withRead("A").build(),
                    new TestCallBuilder("C").withRead("A").build()
            );

            assertThatThrownBy(() -> scheduler.orderActionCalls(input))
                    .isInstanceOf(CyclicDataDependencyException.class);
        }

        @Test
        void testIndirectCycleDetected() {

            List<ActionCallConfig> input = Arrays.asList(
                    new TestCallBuilder("A").withRead("B").build(),
                    new TestCallBuilder("B").withRead("C").build(),
                    new TestCallBuilder("C").withRead("A").build()
            );

            assertThatThrownBy(() -> scheduler.orderActionCalls(input))
                    .isInstanceOf(CyclicDataDependencyException.class);
        }


        @Test
        void testWriteCustomization() throws Exception {

            List<ActionCallConfig> input = Arrays.asList(
                    new TestCallBuilder("A").withRead("B").build(),
                    new TestCallBuilder("B").withWrite("C").withRemovedWrite("B").build(),
                    new TestCallBuilder("A1").withRead("C").build()
            );

            List<String> result = getNames(scheduler.orderActionCalls(input));
            assertThat(result).containsExactly("A", "B", "A1");
        }


        @Test
        void verifyRulesRespected() throws Exception {

            List<ActionCallConfig> input = Arrays.asList(
                    new TestCallBuilder("read_propagated")
                            .withDataInput("http_path")
                            .withRead("http_path")
                            .withReadsBeforeWritten("http_path")
                            .build(),
                    new TestCallBuilder("http_path")
                            .withRead("something")
                            .build(),
                    new TestCallBuilder("A_parametrize_http")
                            .withRead("http_path")
                            .withWrite("http_path")
                            .build(),
                    new TestCallBuilder("B_parametrize_http")
                            .withRead("http_path")
                            .withWrite("http_path")
                            .build(),
                    new TestCallBuilder("read_parametrized")
                            .withDataInput("http_path")
                            .build()

            );

            List<String> result = getNames(scheduler.orderActionCalls(input));
            assertThat(result).containsExactly("read_propagated", "http_path", "A_parametrize_http", "B_parametrize_http", "read_parametrized");
        }

    }
}
