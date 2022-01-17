package rocks.inspectit.ocelot.rest.yamlhighlighter;

import com.google.common.base.CaseFormat;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.MediaType;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.rest.AbstractBaseController;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController()
public class HighlightRulesMapController extends AbstractBaseController {

    private static Map<String, Object> generateMap(Class<?> currentClass){
        Map<String, Object> currentClassMap = new HashMap<>();

        ReflectionUtils.doWithFields(currentClass, field -> {

            if(Modifier.isStatic(field.getModifiers())){
                return;
            }

            Map<String, Object> innerMap = new HashMap<>();
            if (field.getType().equals(java.util.Map.class)){

                innerMap.put("type", "map");
                ParameterizedType type = (ParameterizedType) field.getGenericType();
                Type valueType = type.getActualTypeArguments()[1];
                if(valueType.equals(java.lang.Object.class)) {
                    innerMap.put("map-content-type", "yaml");
                } else if(valueType.getTypeName().startsWith("rocks.inspectit.ocelot.config.model")){
                    innerMap.put("map-content-type", "object");
                    innerMap.put("map-contents", generateMap((Class<?>) valueType));
                } else {
                    innerMap.put("map-content-type", "text");
                }

            } else if (field.getType().equals(java.util.List.class)){

                innerMap.put("type", "list");
                ParameterizedType type = (ParameterizedType) field.getGenericType();
                Type listContentType = type.getActualTypeArguments()[0];

                if(listContentType.equals(String.class)){
                    innerMap.put("list-content-type", "text");
                } else {
                    innerMap.put("list-content-type", "object");
                    innerMap.put("list-contents", generateMap((Class<?>) listContentType));
                }

            } else if (field.getType().equals(java.lang.Object.class)){

                innerMap.put("type", "yaml");

            } else if(field.getType().getName().startsWith("rocks.inspectit.ocelot.config.model")){

                if(field.getType().isEnum()){
                    List<String> enumValues = new ArrayList<>();
                    for(Field enumField: field.getType().getFields()){
                        enumValues.add(enumField.getName());
                    }
                    innerMap.put("type", "enum");
                    innerMap.put("enum-values", enumValues);
                } else {
                    innerMap.put("type", "object");
                    innerMap.put("object-attributes", generateMap(field.getType()));
                }

            } else {

                innerMap.put("type", "text");

            }
            currentClassMap.put(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, field.getName()), innerMap);
        });

        return currentClassMap;
    }

    @ApiOperation(value = "Get JSON for Highlighting Rules Generation", notes = "")
    @GetMapping(value = "highlightRulesMap", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getHighlightRulesMap() {

        Map<String, Object> mainMap = new HashMap<>();
        Map<String, Object> innerMap = new HashMap<>();
        Map<String, Object> innerInnerMap = new HashMap<>();
        Map<String, Object> innerInnerInnerMap = new HashMap<>();
        innerInnerInnerMap.put("type", "object");
        innerInnerInnerMap.put("object-attributes", generateMap(InspectitConfig.class));
        innerInnerMap.put("inspectit", innerInnerInnerMap);
        innerMap.put("object-attributes", innerInnerMap);
        innerMap.put("type", "object");
        mainMap.put("start", innerMap);

        return  mainMap;
    }

}
