package rocks.inspectit.oce.eum.server.arithmetic;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;

class ArithmeticExpressionTest {

    @Nested
    public class Eval {

        @Test
        public void evaulateMinus() {
            ArithmeticExpression expression = new ArithmeticExpression("1565601241723 - 1565601241693");

            double result = expression.eval();

            assertThat(result).isEqualTo(30);
        }

        @Test
        public void evaulatePlus() {
            ArithmeticExpression expression = new ArithmeticExpression("10 + 20");

            double result = expression.eval();

            assertThat(result).isEqualTo(30);
        }

        @Test
        public void evaulateParentheses() {
            ArithmeticExpression expression = new ArithmeticExpression("(1+1)*5");

            double result = expression.eval();

            assertThat(result).isEqualTo(10);
        }
    }
}