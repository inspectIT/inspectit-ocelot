package rocks.inspectit.ocelot.rest.yamlhighlighter;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CaseFormat;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.MediaType;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.instrumentation.actions.GenericActionSettings;
import rocks.inspectit.ocelot.rest.AbstractBaseController;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
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

    @VisibleForTesting
    Map<String, Object> generateMap(Class<?> currentClass) {
        Map<String, Object> currentClassMap = new HashMap<>();

        ReflectionUtils.doWithFields(currentClass, field -> {

            if (Modifier.isStatic(field.getModifiers())) {
                return;
            }

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

                innerMap.put(KEY_TYPE, VALUE_TYPE_YAML);

            } else if (field.getType().isEnum()) {
                innerMap.put(KEY_TYPE, VALUE_TYPE_ENUM);
                innerMap.put(KEY_ENUM_VALUES, extractEnumValues(field.getType()));

            } else if (field.getType().getName().startsWith("rocks.inspectit.ocelot.config.model")) {

                innerMap.put(KEY_TYPE, VALUE_TYPE_OBJECT);
                innerMap.put(KEY_OBJECT_ATTRIBUTES, generateMap(field.getType()));

            } else if (currentClass.equals(GenericActionSettings.class) && (field.getName()
                    .equals("value") || field.getName().equals("valueBody"))) {

                innerMap.put(KEY_TYPE, VALUE_TYPE_JAVA);

            } else {

                innerMap.put(KEY_TYPE, VALUE_TYPE_TEXT);

            }
            currentClassMap.put(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, field.getName()), innerMap);
        });

        return currentClassMap;
    }

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

    private List<String> extractEnumValues(Class<?> currentEnum) {
        List<String> enumValues = new ArrayList<>();
        for (Field enumField : currentEnum.getFields()) {
            enumValues.add(enumField.getName());
        }
        return enumValues;
    }

    @ApiOperation(value = "Get JSON for Highlight Rules Generation", notes = "")
    @GetMapping(value = "highlight-rules", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getHighlightRulesMap() {
        return generateMap(InspectitConfig.class);
    }

}
