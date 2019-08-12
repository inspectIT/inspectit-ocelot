package rocks.inspectit.oce.eum.server.arithmetic;

import com.google.common.annotations.VisibleForTesting;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.oce.eum.server.beacon.Beacon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class RawExpression {

    private static final Pattern pattern = Pattern.compile("\\{([^}]+)\\}");

    private String expression;

    private List<String> fields;

    @Getter
    private boolean isSelectionExpression;

    public RawExpression(String expression) {
        this.expression = removeWhitespaces(expression);

        parse();
    }

    private String removeWhitespaces(String input) {
        return input.replaceAll("\\s+", "");
    }

    private void parse() {
        extractFields();
        isSelectionExpression = expression.matches("\\{([^}]+)\\}");
    }

    private void extractFields() {
        Matcher matcher = pattern.matcher(expression);
        List<String> fieldList = new ArrayList<>();

        while (matcher.find()) {
            String field = matcher.group(1);
            if (!fieldList.contains(field)) {
                fieldList.add(field);
            }
        }

        this.fields = Collections.unmodifiableList(fieldList);
    }

    @VisibleForTesting
    List<String> getFields() {
        return fields;
    }

    public boolean isSolvable(Beacon beacon) {
        return beacon.contains(fields);
    }

    public Number solve(Beacon beacon) {
        if (isSelectionExpression) {
            double value = Double.parseDouble(beacon.get(fields.get(0)));
            log.info("Directly returning '{}' for expression '{}'.", value, fields.get(0));
            return value;
        }

        String resolvedExpression = expression;
        for (String field : fields) {
            String fieldValue = beacon.get(field);
            resolvedExpression = resolvedExpression.replace("{" + field + "}", fieldValue);
        }

        double value = new ArithmeticExpression(resolvedExpression).eval();

        log.info("Resolved expression '{}' to '{}' resulting in '{}'.", expression, resolvedExpression, value);

        return value;
    }
}
