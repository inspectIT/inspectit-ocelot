package rocks.inspectit.ocelot.core.service;

import lombok.val;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.config.model.InspectitConfig;

/**
 * Condition to be applied with {@link org.springframework.context.annotation.Conditional}.
 * This condition checks if the {@link ActivationConfigCondition} attached to the same class
 * evaluates to "true".
 */
public class ActivationConfigConditionMet implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        InspectitEnvironment env = (InspectitEnvironment) context.getEnvironment();
        val attribs = metadata.getAnnotationAttributes(ActivationConfigCondition.class.getName());
        if (attribs == null || attribs.isEmpty()) {
            throw new RuntimeException("Annotation ActivationConfigCondition is missing!");
        }
        InspectitConfig currentConfig = env.getCurrentConfig();

        ExpressionParser parser = new SpelExpressionParser();
        Expression exp = parser.parseExpression((String) attribs.get("value"));
        return (Boolean) exp.getValue(currentConfig);
    }
}
