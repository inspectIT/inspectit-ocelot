package rocks.inspectit.oce.eum.server.arithmetic;

/**
 * Class used to solve arithmetic expressions.
 * It is mainly based on the following StackOverflow answer by user "Boann".
 * See the following answer: https://stackoverflow.com/a/26227947/2478009
 * <p>
 * Grammar:
 * * expression = term | expression `+` term | expression `-` term
 * * term = factor | term `*` factor | term `/` factor
 * * factor = `+` factor | `-` factor | `(` expression `)` | number | functionName factor | factor `^` factor
 */
public class ArithmeticExpression {

    /**
     * The expression string.
     */
    private final String expression;

    private int pos = -1;

    private int ch;

    /**
     * Constructor.
     *
     * @param expression the expression to solve
     */
    public ArithmeticExpression(String expression) {
        this.expression = expression;
    }

    private void nextChar() {
        ch = (++pos < expression.length()) ? expression.charAt(pos) : -1;
    }

    private boolean eat(int charToEat) {
        while (ch == ' ') nextChar();
        if (ch == charToEat) {
            nextChar();
            return true;
        }
        return false;
    }

    private double parse() {
        nextChar();
        double x = parseExpression();
        if (pos < expression.length()) throw new RuntimeException("Unexpected: " + (char) ch);
        return x;
    }

    private double parseExpression() {
        double x = parseTerm();
        for (; ; ) {
            if (eat('+')) x += parseTerm(); // addition
            else if (eat('-')) x -= parseTerm(); // subtraction
            else return x;
        }
    }

    private double parseTerm() {
        double x = parseFactor();
        for (; ; ) {
            if (eat('*')) x *= parseFactor(); // multiplication
            else if (eat('/')) x /= parseFactor(); // division
            else return x;
        }
    }

    private double parseFactor() {
        if (eat('+')) return parseFactor(); // unary plus
        if (eat('-')) return -parseFactor(); // unary minus

        double x;
        int startPos = this.pos;
        if (eat('(')) { // parentheses
            x = parseExpression();
            eat(')');
        } else if ((ch >= '0' && ch <= '9') || ch == '.') { // numbers
            while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
            x = Double.parseDouble(expression.substring(startPos, this.pos));
        } else if (ch >= 'a' && ch <= 'z') { // functions
            while (ch >= 'a' && ch <= 'z') nextChar();
            String func = expression.substring(startPos, this.pos);
            x = parseFactor();
            if (func.equals("sqrt")) x = Math.sqrt(x);
            else if (func.equals("sin")) x = Math.sin(Math.toRadians(x));
            else if (func.equals("cos")) x = Math.cos(Math.toRadians(x));
            else if (func.equals("tan")) x = Math.tan(Math.toRadians(x));
            else throw new RuntimeException("Unknown function: " + func);
        } else {
            throw new ArithmeticException("Could not solve expression '" + expression + "'. Unexpected character at position " + startPos + ": " + (char) ch);
        }

        if (eat('^')) x = Math.pow(x, parseFactor()); // exponentiation

        return x;
    }

    /**
     * Evaluates the expression.
     *
     * @return the result of the expression
     */
    public double eval() {
        return parse();
    }
}
