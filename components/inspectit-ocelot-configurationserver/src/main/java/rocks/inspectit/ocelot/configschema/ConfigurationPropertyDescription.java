package rocks.inspectit.ocelot.configschema;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import rocks.inspectit.ocelot.config.ui.UISettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * A description of a "plain" configuration property of the inspectit configuration ({@link rocks.inspectit.ocelot.config.model.InspectitConfig}).
 * These descriptions is designed to generate a UI mask for configuring the properties.
 * <p>
 * Descriptions are not available for all properties, but only for "plain" ones.
 * Currently this excludes any property which is part of a map or a list.
 * <p>
 * The description implements the {@link Comparable} with a custom order.
 * The order is that all non-composite properties appear before composite ones.
 * On the second level all descriptions are sorted by their name.
 */
@Value
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ConfigurationPropertyDescription implements Comparable<ConfigurationPropertyDescription> {

    /**
     * The type of the property.
     */
    private final ConfigurationPropertyType type;

    /**
     * The human readable name of the property. Can be specified by the {@link UISettings} annotation.
     * If the annotation is not present, the name is generated from the name of the property field.
     */
    private final String readableName;

    /**
     * The kebap-case name of the property. This name is used to specify values for it in configuration files.
     */
    private final String propertyName;

    /**
     * True, if "null" is a valid value for the given property.
     */
    private final boolean nullable;

    /**
     * The child properties in case {@link #type} is {@link ConfigurationPropertyType#COMPOSITE}.
     * Otherwise this List is always empty.
     * <p>
     * In addition the list is guaranteed to be sorted.
     */
    private final List<ConfigurationPropertyDescription> children;

    /**
     * The possible values in case the {@link #type} is {@link ConfigurationPropertyType#ENUM}.
     */
    private final List<String> enumValues;

    @Builder
    private ConfigurationPropertyDescription(ConfigurationPropertyType type, String propertyName, String readableName, boolean nullable, @Singular List<ConfigurationPropertyDescription> children, @Singular List<String> enumValues) {
        this.type = type;
        this.propertyName = propertyName;
        this.readableName = Optional.ofNullable(readableName).orElse(propertyName);
        this.nullable = nullable;
        this.children = new ArrayList<>();
        if (children != null) {
            this.children.addAll(children);
            Collections.sort(this.children);
        }
        this.enumValues = new ArrayList<>();
        if (enumValues != null) {
            this.enumValues.addAll(enumValues);
            Collections.sort(this.enumValues);
        }
    }

    /**
     * Orders property descriptions so that composite ones appear after other ones.
     * On the second level, they are ordered lexicographically by name.
     *
     * @param other the other instance to compare to
     *
     * @return -1 if this is less, 0 if the name is equal and 1 if this is greater.
     */
    @Override
    public int compareTo(ConfigurationPropertyDescription other) {
        if (type == other.type) {
            return readableName.compareTo(other.readableName);
        } else {
            if (type == ConfigurationPropertyType.COMPOSITE) {
                return 1;
            } else if (other.type == ConfigurationPropertyType.COMPOSITE) {
                return -1;
            } else {
                return readableName.compareTo(other.readableName);
            }
        }
    }
}
