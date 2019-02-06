package rocks.inspectit.oce.core.config.model.validation;

import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class AdditionalValidationsValidator implements ConstraintValidator<AdditionalValidations, Object> {

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();
        Class<?> clazz = value.getClass();

        List<Violation> foundViolations = new ArrayList<>();

        Arrays.stream(clazz.getMethods())
                .filter(m -> m.isAnnotationPresent(AdditionalValidation.class))
                .filter(m -> Arrays.stream(m.getParameters())
                        .map(Parameter::getType)
                        .collect(toList()).equals(Arrays.asList(ViolationBuilder.class)))
                .forEach(m -> invokeAdditionalValidationMethod(value, foundViolations, m));

        HibernateConstraintValidatorContext ctx = context.unwrap(HibernateConstraintValidatorContext.class);
        buildViolations(foundViolations, ctx);
        return foundViolations.isEmpty();
    }

    private void buildViolations(List<Violation> foundViolations, HibernateConstraintValidatorContext ctx) {
        for (Violation vio : foundViolations) {
            vio.getParameters().forEach(ctx::addMessageParameter);
            if (vio.getBeanNodes().isEmpty()) {
                ctx.buildConstraintViolationWithTemplate(vio.getMessage()).addConstraintViolation();
            } else {
                ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext current = null;
                for (String node : vio.getBeanNodes()) {
                    if (current == null) {
                        current = ctx.buildConstraintViolationWithTemplate(vio.getMessage()).addPropertyNode(node);
                    } else {
                        current = current.addPropertyNode(node);
                    }
                }
                current.addConstraintViolation();
            }
        }
    }

    private void invokeAdditionalValidationMethod(Object bean, List<Violation> foundViolations, Method m) {
        String defaultMsg = m.getAnnotation(AdditionalValidation.class).value();
        try {
            m.invoke(bean, new ViolationBuilder(foundViolations).message(defaultMsg));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
