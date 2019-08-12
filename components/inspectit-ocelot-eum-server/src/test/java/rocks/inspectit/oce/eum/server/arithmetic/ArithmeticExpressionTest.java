package rocks.inspectit.oce.eum.server.arithmetic;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;

class ArithmeticExpressionTest {

    @Nested
    public class Eval {

        @Test
        public void t() {
            ArithmeticExpression expression = new ArithmeticExpression("1565601241723 - 1565601241693");

            double result = expression.eval();

            assertThat(result).isEqualTo(30);
        }
    }
}