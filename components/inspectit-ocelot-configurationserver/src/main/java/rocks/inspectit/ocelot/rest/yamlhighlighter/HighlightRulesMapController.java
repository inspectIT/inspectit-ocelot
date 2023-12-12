package rocks.inspectit.ocelot.rest.yamlhighlighter;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CaseFormat;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.instrumentation.actions.GenericActionSettings;
import rocks.inspectit.ocelot.rest.AbstractBaseController;

import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class HighlightRulesMapController extends AbstractBaseController {

    @VisibleForTesting
    static final String VALUE_TYPE_MAP = "map";

    @VisibleForTesting
    static final String VALUE_TYPE_YAML = "yaml";

    @VisibleForTesting
    static final String VALUE_TYPE_JAVA = "java";

    @VisibleForTesting
    static final String VALUE_TYPE_TEXT = "text";

    @VisibleForTesting
    static final String VALUE_TYPE_LIST = "list";

    @VisibleForTesting
    static final String VALUE_TYPE_OBJECT = "object";

    @VisibleForTesting
    static final String VALUE_TYPE_ENUM = "enum";

    @VisibleForTesting
    static final String KEY_TYPE = "type";

    @VisibleForTesting
    static final String KEY_MAP_CONTENT_TYPE = "map-content-type";

    @VisibleForTesting
    static final String KEY_MAP_CONTENTS = "map-contents";

    @VisibleForTesting
    static final String KEY_LIST_CONTENT_TYPE = "list-content-type";

    @VisibleForTesting
    static final String KEY_LIST_CONTENTS = "list-contents";

    @VisibleForTesting
    static final String KEY_ENUM_VALUES = "enum-values";

    @VisibleForTesting
    static final String KEY_OBJECT_ATTRIBUTES = "object-attributes";

    /**
     * Generates a Map that describes the structure of a class, as of now only used with {@link InspectitConfig},
     * and its fields recursively for usage in Syntax Highlighting in the config-server UI.
     *
     * @param currentClass The class whose fields should be described.
     *
     * @return A Map describing the structure of the class.
     */
    @VisibleForTesting
    Map<String, Object> generateMap(Class<?> currentClass) {
        Map<String, Object> currentClassMap = new HashMap<>();

        ReflectionUtils.doWithFields(currentClass, field -> {

            // Static fields are ignored, since they are not configured in the UI.
            if (Modifier.isStatic(field.getModifiers())) {
                return;
            }

            // Create new map that will contain the data for the current field.
            Map<String, Object> innerMap = new HashMap<>();

            if (field.getType().equals(java.util.Map.class)) {

                innerMap.put(KEY_TYPE, VALUE_TYPE_MAP);
                ParameterizedType type = (ParameterizedType) field.getGenericType();
                Class<?> valueType = (Class<?>) type.getActualTypeArguments()[1];

                generateMapCollections(innerMap, valueType, KEY_MAP_CONTENT_TYPE, KEY_MAP_CONTENTS);

            } else if (field.getType().equals(java.util.List.class)) {

                innerMap.put(KEY_TYPE, VALUE_TYPE_LIST);
                ParameterizedType type = (ParameterizedType) field.getGenericType();
                Class<?> listContentType = (Class<?>) type.getActualTypeArguments()[0];

                generateMapCollections(innerMap, listContentType, KEY_LIST_CONTENT_TYPE, KEY_LIST_CONTENTS);

            } else if (field.getType().equals(java.lang.Object.class)) {
                // If the field is an Object, we can not make any assumptions about its contents and
                // instead any arbitrary YAML will be allowed in the highlighting.
                innerMap.put(KEY_TYPE, VALUE_TYPE_YAML);

            } else if (field.getType().isEnum()) {

                innerMap.put(KEY_TYPE, VALUE_TYPE_ENUM);
                innerMap.put(KEY_ENUM_VALUES, extractEnumValues(field.getType()));

            } else if (field.getType().getName().startsWith("rocks.inspectit.ocelot.config.model")) {
                // If the field is of a class of this project, this method will be called again recursively to describe
                // the contents of that class as well.
                innerMap.put(KEY_TYPE, VALUE_TYPE_OBJECT);
                innerMap.put(KEY_OBJECT_ATTRIBUTES, generateMap(field.getType()));

            } else if (currentClass.equals(GenericActionSettings.class) && (field.getName()
                    .equals("value") || field.getName().equals("valueBody"))) {
                // These two fields contain a special kind of String, i.e. Java code, so they get a special type for highlighting.
                innerMap.put(KEY_TYPE, VALUE_TYPE_JAVA);

            } else {
                // If the field is of any other class, any contents will simply be highlighted as text.
                innerMap.put(KEY_TYPE, VALUE_TYPE_TEXT);

            }
            currentClassMap.put(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, field.getName()), innerMap);
        });

        return currentClassMap;
    }

    /**
     * Helper function for {@link this#generateMap} to set contents for collections in the described classes,
     * i.e. Lists or Maps. Works analogous to {@link this#generateMap}.
     *
     * @param innerMap       The inner map created in {@link this#generateMap} for the current field.
     * @param contentType    The type of the values of the Collection.
     * @param keyContentType Key for the content-type in the generated map, e.g. KEY_MAP_CONTENT_TYPE.
     * @param keyContents    Key for the contents in the generated map, e.g. KEY_MAP_CONTENTS.
     */
    private void generateMapCollections(Map<String, Object> innerMap, Class<?> contentType, String keyContentType, String keyContents) {
        if (contentType.equals(Object.class)) {
            innerMap.put(keyContentType, VALUE_TYPE_YAML);
        } else if (contentType.isEnum()) {
            innerMap.put(keyContentType, VALUE_TYPE_ENUM);
            innerMap.put(KEY_ENUM_VALUES, extractEnumValues(contentType));
        } else if (contentType.getTypeName().startsWith("rocks.inspectit.ocelot.config.model")) {
            innerMap.put(keyContentType, VALUE_TYPE_OBJECT);
            innerMap.put(keyContents, generateMap(contentType));
        } else {
            innerMap.put(keyContentType, VALUE_TYPE_TEXT);
        }
    }

    /**
     * Returns a list of names of all possible values for the given enum.
     *
     * @param currentEnum Enum whose values should be returned.
     *
     * @return List of names of all possible values for the enum.
     */
    private List<String> extractEnumValues(Class<?> currentEnum) {
        return Arrays.stream(currentEnum.getEnumConstants()).map(Object::toString).collect(Collectors.toList());
    }

    @Operation(summary = "Get JSON for Highlight Rules Generation", description = "")
    @GetMapping(value = {"highlight-rules", "highlight-rules/"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getHighlightRulesMap() {
        return generateMap(InspectitConfig.class);
    }

}
