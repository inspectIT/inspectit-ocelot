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
import java.lang.reflect.Type;
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
    static final String KEY_START = "start";

    @VisibleForTesting
    static final String KEY_INSPECTIT = "inspectit";

    @VisibleForTesting
    static Map<String, Object> generateMap(Class<?> currentClass) {
        Map<String, Object> currentClassMap = new HashMap<>();

        ReflectionUtils.doWithFields(currentClass, field -> {

            if (Modifier.isStatic(field.getModifiers())) {
                return;
            }

            Map<String, Object> innerMap = new HashMap<>();

            if (field.getType().equals(java.util.Map.class)) {

                innerMap.put(KEY_TYPE, VALUE_TYPE_MAP);
                ParameterizedType type = (ParameterizedType) field.getGenericType();
                Type valueType = type.getActualTypeArguments()[1];
                if (valueType.equals(java.lang.Object.class)) {
                    innerMap.put(KEY_MAP_CONTENT_TYPE, VALUE_TYPE_YAML);
                } else if (valueType.getTypeName().startsWith("rocks.inspectit.ocelot.config.model")) {
                    innerMap.put(KEY_MAP_CONTENT_TYPE, VALUE_TYPE_OBJECT);
                    innerMap.put(KEY_MAP_CONTENTS, generateMap((Class<?>) valueType));
                } else {
                    innerMap.put(KEY_MAP_CONTENT_TYPE, VALUE_TYPE_TEXT);
                }

            } else if (field.getType().equals(java.util.List.class)) {

                innerMap.put(KEY_TYPE, VALUE_TYPE_LIST);
                ParameterizedType type = (ParameterizedType) field.getGenericType();
                Type listContentType = type.getActualTypeArguments()[0];

                if (listContentType.equals(String.class)) {
                    innerMap.put(KEY_LIST_CONTENT_TYPE, VALUE_TYPE_TEXT);
                } else {
                    innerMap.put(KEY_LIST_CONTENT_TYPE, VALUE_TYPE_OBJECT);
                    innerMap.put(KEY_LIST_CONTENTS, generateMap((Class<?>) listContentType));
                }

            } else if (field.getType().equals(java.lang.Object.class)) {

                innerMap.put(KEY_TYPE, VALUE_TYPE_YAML);

            } else if (field.getType().getName().startsWith("rocks.inspectit.ocelot.config.model")) {

                if (field.getType().isEnum()) {
                    List<String> enumValues = new ArrayList<>();
                    for (Field enumField : field.getType().getFields()) {
                        enumValues.add(enumField.getName());
                    }
                    innerMap.put(KEY_TYPE, VALUE_TYPE_ENUM);
                    innerMap.put(KEY_ENUM_VALUES, enumValues);
                } else {
                    innerMap.put(KEY_TYPE, VALUE_TYPE_OBJECT);
                    innerMap.put(KEY_OBJECT_ATTRIBUTES, generateMap(field.getType()));
                }

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

    @ApiOperation(value = "Get JSON for Highlight Rules Generation", notes = "")
    @GetMapping(value = "highlight-rules", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getHighlightRulesMap() {
        return  generateMap(InspectitConfig.class);
    }

}
