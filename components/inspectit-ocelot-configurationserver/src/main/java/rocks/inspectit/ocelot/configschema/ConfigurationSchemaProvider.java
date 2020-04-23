package rocks.inspectit.ocelot.configschema;

import com.google.common.annotations.VisibleForTesting;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.ui.UISettings;
import rocks.inspectit.ocelot.config.utils.CaseUtils;
import rocks.inspectit.ocelot.config.validation.PropertyPathHelper;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Builds a configuration for all "plain" properties the {@link InspectitConfig} contains.
 */
@Component
public class ConfigurationSchemaProvider {

    private static final Set<Class<?>> INTEGER_CLASSES = new HashSet<>(Arrays.asList(
            Byte.class, byte.class, Short.class, short.class, Integer.class, int.class, Long.class, long.class
    ));

    private static final Set<Class<?>> FLOAT_CLASSES = new HashSet<>(Arrays.asList(
            Float.class, float.class, Double.class, double.class
    ));

    private static final Set<Class<?>> BOOLEAN_CLASSES = new HashSet<>(Arrays.asList(
            Boolean.class, boolean.class
    ));


    private ConfigurationPropertyDescription rootProperty;

    /**
     * @return the description of the "inspectit" root configuration proeprty which corresponds to the {@link InspectitConfig} class.
     */
    public ConfigurationPropertyDescription getSchema() {
        return rootProperty;
    }

    @PostConstruct
    void deriveSchema() {
        rootProperty = ConfigurationPropertyDescription.builder()
                .propertyName("inspectit")
                .children(getCompositePropertyChildren(InspectitConfig.class))
                .type(ConfigurationPropertyType.COMPOSITE)
                .build();
    }

    private List<ConfigurationPropertyDescription> getCompositePropertyChildren(Class<?> beanClass) {
        return Arrays.stream(BeanUtils.getPropertyDescriptors(beanClass))
                .map(this::toDescription)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private boolean isExcluded(PropertyDescriptor property) {
        return getPropertyAnnotation(property, UISettings.class).map(UISettings::exclude).orElse(false);
    }

    @VisibleForTesting
    Optional<ConfigurationPropertyDescription> toDescription(PropertyDescriptor property) {
        if (property.getWriteMethod() != null && !isExcluded(property)) {
            Type type = property.getWriteMethod().getGenericParameterTypes()[0];
            String name = CaseUtils.camelCaseToKebabCase(property.getName());
            ConfigurationPropertyDescription.ConfigurationPropertyDescriptionBuilder builder =
                    ConfigurationPropertyDescription.builder()
                            .propertyName(name)
                            .readableName(getReadableName(property))
                            .nullable(isNullable(property));

            if (PropertyPathHelper.isBean(type)) {
                return Optional.of(
                        builder
                                .type(ConfigurationPropertyType.COMPOSITE)
                                .children(getCompositePropertyChildren((Class<?>) type))
                                .build()
                );
            } else if (PropertyPathHelper.isTerminal(type)) {
                return Optional.of(setTerminalType(type, builder).build());
            }

        }
        return Optional.empty();
    }

    private String getReadableName(PropertyDescriptor property) {
        return getPropertyAnnotation(property, UISettings.class)
                .map(UISettings::name)
                .orElse(camelCaseToReadableName(property.getName()));
    }

    @VisibleForTesting
    ConfigurationPropertyDescription.ConfigurationPropertyDescriptionBuilder
    setTerminalType(Type type, ConfigurationPropertyDescription.ConfigurationPropertyDescriptionBuilder builder) {
        if (type instanceof Class<?>) {
            Class<?> clazz = (Class<?>) type;
            if (clazz.isEnum()) {
                return builder
                        .type(ConfigurationPropertyType.ENUM)
                        .enumValues(Arrays.stream(clazz.getEnumConstants()).map(Object::toString)
                                .collect(Collectors.toSet()));
            } else if (INTEGER_CLASSES.contains(clazz)) {
                return builder.type(ConfigurationPropertyType.INTEGER);
            } else if (FLOAT_CLASSES.contains(clazz)) {
                return builder.type(ConfigurationPropertyType.FLOAT);
            } else if (BOOLEAN_CLASSES.contains(clazz)) {
                return builder.type(ConfigurationPropertyType.BOOLEAN);
            } else if (Duration.class.isAssignableFrom(clazz)) {
                return builder.type(ConfigurationPropertyType.DURATION);
            } else {
                return builder.type(ConfigurationPropertyType.STRING);
            }
        } else {
            throw new IllegalArgumentException("Unexpected type: " + type);
        }
    }

    private boolean isNullable(PropertyDescriptor property) {
        Class<?> type = property.getWriteMethod().getParameterTypes()[0];
        if (type.isPrimitive()) {
            return false;
        }
        return !getPropertyAnnotation(property, NotNull.class).isPresent()
                && !getPropertyAnnotation(property, NotBlank.class).isPresent()
                && !getPropertyAnnotation(property, NotEmpty.class).isPresent();
    }

    private <T extends Annotation> Optional<T> getPropertyAnnotation(PropertyDescriptor property, Class<T> annotationClass) {
        Method writeMethod = property.getWriteMethod();
        if (writeMethod != null) {
            try {
                Field field = writeMethod.getDeclaringClass().getDeclaredField(property.getName());
                T annotation = field.getAnnotation(annotationClass);
                if (annotation != null) {
                    return Optional.of(annotation);
                }
            } catch (NoSuchFieldException e) {
            }
            return Optional.ofNullable(writeMethod.getAnnotation(annotationClass));
        }
        return Optional.empty();
    }

    private String camelCaseToReadableName(String camelCase) {
        String kebap = CaseUtils.camelCaseToKebabCase(camelCase);
        return Arrays.stream(kebap.split("-"))
                .map(str -> Character.toUpperCase(str.charAt(0)) + str.substring(1))
                .collect(Collectors.joining(" "));
    }
}
