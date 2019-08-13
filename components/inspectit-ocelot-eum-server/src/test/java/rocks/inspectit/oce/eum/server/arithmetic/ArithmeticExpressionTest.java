package rocks.inspectit.oce.eum.server.arithmetic;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Java6Assertions.assertThat;

class ArithmeticExpressionTest {

    @Nested
    public class Eval {

        @Test
        public void evaluateMinus() {
            ArithmeticExpression expression = new ArithmeticExpression("1565601241723 - 1565601241693");

            double result = expression.eval();

            assertThat(result).isEqualTo(30);
        }

        @Test
        public void evuluatePlus() {
            ArithmeticExpression expression = new ArithmeticExpression("10 + 20");

            double result = expression.eval();

            assertThat(result).isEqualTo(30);
        }

        @Test
        public void evaluateParentheses() {
            ArithmeticExpression expression = new ArithmeticExpression("(1+1)*5");

            double result = expression.eval();

            assertThat(result).isEqualTo(10);
        }

        @Test
        public void invalidExpression() {
            ArithmeticExpression expression = new ArithmeticExpression("(1+*5");

            assertThatExceptionOfType(RuntimeException.class)
                    .isThrownBy(expression::eval)
                    .withMessage("Could not solve expression '(1+*5'. Unexpected character at position 3: *");
        }
    }
}