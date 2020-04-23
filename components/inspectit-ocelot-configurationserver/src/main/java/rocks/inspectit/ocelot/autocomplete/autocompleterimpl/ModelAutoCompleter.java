package rocks.inspectit.ocelot.autocomplete.autocompleterimpl;

import com.google.common.annotations.VisibleForTesting;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import rocks.inspectit.ocelot.autocomplete.AutoCompleter;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.utils.CaseUtils;
import rocks.inspectit.ocelot.config.validation.PropertyPathHelper;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ModelAutoCompleter implements AutoCompleter {

    @Override
    public List<String> getSuggestions(List<String> path) {
        if (CollectionUtils.isEmpty(path) || (path.size() == 1 && path.get(0).equals(""))) {
            return Collections.singletonList("inspectit");
        } else if (path.size() >= 1 && !path.get(0).equals("inspectit")) {
            return Collections.emptyList();
        }
        return collectProperties(path.subList(1, path.size()));
    }

    /**
     * Returns the names of the properties in a given path
     *
     * @param propertyPath The path to a property one wants to recieve the properties of
     * @return The names of the properties of the given path as list
     */
    private List<String> collectProperties(List<String> propertyPath) {
        Type endType = PropertyPathHelper.getPathEndType(propertyPath, InspectitConfig.class);
        if (CollectionUtils.isEmpty(propertyPath) || ((propertyPath.size() == 1) && propertyPath.get(0).equals(""))) {
            return getProperties(InspectitConfig.class);
        }
        if (endType == null || PropertyPathHelper.isTerminal(endType) || PropertyPathHelper.isListOfTerminalTypes(endType) || !(endType instanceof Class<?>)) {
            return Collections.emptyList();
        }
        return getProperties((Class<?>) endType);
    }

    /**
     * Return the properties of a given class
     *
     * @param beanClass the class one wants the properties of
     * @return the properties of the given class
     */
    @VisibleForTesting
    List<String> getProperties(Class<?> beanClass) {
        return Arrays.stream(BeanUtils.getPropertyDescriptors(beanClass))
                .filter(propertyDescriptor -> propertyDescriptor.getWriteMethod() != null)
                .map(PropertyDescriptor::getName)
                .map(CaseUtils::camelCaseToKebabCase)
                .collect(Collectors.toList());
    }
}
