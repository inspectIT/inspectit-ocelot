package rocks.inspectit.oce.eum.server.arithmetic;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rocks.inspectit.oce.eum.server.beacon.Beacon;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RawExpressionTest {

    @Nested
    public class T {

        @Test
        public void t0() {
            RawExpression expression = new RawExpression("2");

            assertThat(expression.getFields()).isEmpty();
            assertThat(expression.isSelectionExpression()).isFalse();
        }

        @Test
        public void t() {
            RawExpression expression = new RawExpression("{field}");

            assertThat(expression.getFields()).containsExactly("field");
            assertThat(expression.isSelectionExpression()).isTrue();
        }

        @Test
        public void t2() {
            RawExpression expression = new RawExpression("{field} - {field.second}");

            assertThat(expression.getFields()).containsExactly("field", "field.second");
            assertThat(expression.isSelectionExpression()).isFalse();
        }

        @Test
        public void t3() {
            RawExpression expression = new RawExpression("{field} - {field}");

            assertThat(expression.getFields()).containsExactly("field");
            assertThat(expression.isSelectionExpression()).isFalse();
        }
    }

    @Nested
    public class IsSolvable {

        @Test
        public void t0() {
            Beacon beacon = Beacon.of(Collections.singletonMap("field", "5"));
            RawExpression expression = new RawExpression("{field} - {field.second}");

            boolean result = expression.isSolvable(beacon);

            assertThat(result).isFalse();
        }

        @Test
        public void t1() {
            Map<String, String> map = new HashMap<>();
            map.put("field", "5");
            map.put("field.second", "10");
            Beacon beacon = Beacon.of(map);
            RawExpression expression = new RawExpression("{field} - {field.second}");

            boolean result = expression.isSolvable(beacon);

            assertThat(result).isTrue();
        }
    }
    @Nested
    public class Solve {

        @Test
        public void t0() {
            Map<String, String> map = new HashMap<>();
            map.put("field", "5");
            map.put("field.second", "10");
            Beacon beacon = Beacon.of(map);
            RawExpression expression = new RawExpression("({field.second} - {field}) * 2");

            Number result = expression.solve(beacon);

            assertThat(result).isEqualTo(10D);
        }

        @Test
        public void t1() {
            Map<String, String> map = new HashMap<>();
            map.put("field", "5");
            map.put("field.second", "10");
            Beacon beacon = Beacon.of(map);
            RawExpression expression = new RawExpression("{field.second}");

            Number result = expression.solve(beacon);

            assertThat(result).isEqualTo(10D);
        }

    }
}