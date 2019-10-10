package rocks.inspectit.oce.eum.server.arithmetic;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.oce.eum.server.beacon.Beacon;
import rocks.inspectit.oce.eum.server.configuration.model.BeaconMetricDefinitionSettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Used to resolve the value expressions of {@link BeaconMetricDefinitionSettings}s.
 */
@Slf4j
public class RawExpression {

    /**
     * Pattern which is used to match a field in the expression. E.g.: {rt.load}
     */
    private static final Pattern pattern = Pattern.compile("\\{([^}]+)\\}");

    /**
     * The current expression which may contain placeholders.
     */
    private String expression;

    /**
     * List of field keys contained in the {@link #expression}.
     */
    @Getter
    private List<String> fields;

    /**
     * Specifies whether the expression is just referencing a certain field, thus, no calculation is required.
     */
    @Getter
    private boolean isSelectionExpression;

    /**
     * Constructor.
     *
     * @param expression the raw expression
     */
    public RawExpression(String expression) {
        this.expression = removeWhitespaces(expression);

        parse();
    }

    /**
     * Removes all whitespaces of the given input.
     */
    private String removeWhitespaces(String input) {
        return input.replaceAll("\\s+", "");
    }

    /**
     * Parses the expression, extracts all contained fields and checks whether a calculation is required.
     */
    private void parse() {
        extractFields();
        isSelectionExpression = expression.matches("\\{([^}]+)\\}");
    }

    /**
     * Extracts the field keys of the {@link #expression} and stores them in the {@link #fields} list.
     */
    private void extractFields() {
        Matcher matcher = pattern.matcher(expression);
        List<String> fieldList = new ArrayList<>();

        while (matcher.find()) {
            String field = matcher.group(1);
            if (!fieldList.contains(field)) {
                fieldList.add(field);
            }
        }

        fields = Collections.unmodifiableList(fieldList);
    }

    /**
     * Checks whether the expression is solvable using the given beacon. A expression is not solvable if the beacon
     * does not contain all fields referenced by the expression.
     *
     * @param beacon the beacon which should be used to solve the expression
     * @return true in case it would be possible to solve the expression using the given beacon
     */
    public boolean isSolvable(Beacon beacon) {
        return beacon.contains(fields);
    }

    /**
     * Solves the expression using the given beacon.
     *
     * @param beacon the beacon used to solve the expression
     * @return the result of the expression
     */
    public Number solve(Beacon beacon) {
        if (isSelectionExpression) {
            double value = Double.parseDouble(beacon.get(fields.get(0)));
            if (log.isDebugEnabled()) {
                log.debug("Directly returning '{}' for expression '{}'.", value, fields.get(0));
            }
            return value;
        }

        String resolvedExpression = expression;
        for (String field : fields) {
            String fieldValue = beacon.get(field);
            if (fieldValue == null) {
                throw new IllegalStateException("The given beacon does not contain the required field '" + field + "'.");
            }
            resolvedExpression = resolvedExpression.replace("{" + field + "}", fieldValue);
        }

        try {
            double value = new ArithmeticExpression(resolvedExpression).eval();

            if (log.isDebugEnabled()) {
                log.debug("Resolved expression '{}' to '{}' resulting in '{}'.", expression, resolvedExpression, value);
            }

            return value;
        } catch (Exception exception) {
            log.warn("Expression '{}' could not be solved.", resolvedExpression, exception);
            return null;
        }
    }
}
