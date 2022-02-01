package inspectit.ocelot.config.doc.generator;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.ocelot.config.model.InspectitConfig;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class ConfigParser {

    /**
     * Parses YAML describing an InspectitConfig into InspectitConfig object.
     * @param configYaml String in YAML format describing an InspectitConfig.
     * @return InspectitConfig described by given YAML.
     */
    public InspectitConfig parseConfig(String configYaml){

        // config YAMLs contain placeholders that are in the inspectit-ocelot-core project evaluated by Spring-Boot,
        // however here there is no InspectitEnvironment which is used for that, so instead Jackson is used and
        // the placeholders evaluated in replacePlaceholders().
        String cleanedInputString = replacePlaceholders(configYaml);

        //Parse the InspectitConfig from the created YAML String
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        //Add Module to deal with non-standard Duration values in the YAML
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Duration.class, new CustomDurationDeserializer());
        mapper.registerModule(module);

        //In the YAML property-names are kebab-case in the Java objects CamelCase, Jackson can do that conversion
        //with the following line
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);

        ObjectReader reader = mapper.reader().withRootName("inspectit").forType(InspectitConfig.class);
        try {
            return reader.readValue(cleanedInputString);
        } catch (IOException e) {
            log.error("YAML String could not be parsed by Jackson. This is probably caused by an error in the configuration files:");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Replaces placeholders in the format ${placeholder} with their referenced values or just the keys inside as a
     * String if no value can be found.
     * @param configYaml String in YAML format with placeholders.
     * @return The YAML-String with the placeholders replaced.
     */
    protected String replacePlaceholders(String configYaml){

        //deserialize YAML to Map to get the placeholders' values from
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try{
            Map<String, Object> yamlMap = mapper.readValue(configYaml, Map.class);

            //find the first occurence of the ${placeholder} syntax
            int index = configYaml.indexOf("${");
            while (index >= 0) {

                //get String within the curly braces of ${placeholder} expression
                String replacementSubstring = configYaml.substring(index + 2);
                replacementSubstring = replacementSubstring.substring(0, replacementSubstring.indexOf("}"));
                log.debug("Index: " + index +", Current replaced Placeholder:" + replacementSubstring);

                //get keys within the placeholder, e.g. "inspectit" and "service-name" from inspectit.service-name
                List<String> keys = Arrays.asList(replacementSubstring.split("\\."));

                //get the value the placeholder references from within the Map based on the YAML
                String newSubstring = getNestedValue(yamlMap, keys);

                //Replace the placeholder with the found value
                configYaml = configYaml.replace("${" + replacementSubstring + "}", newSubstring);

                //get index of next placeholder
                index = configYaml.indexOf("${");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return configYaml;
    }

    /**
     * Get value as String from within nested Maps within the given Map based on the given keys.
     * @param map Map to get value from.
     * @param keys List of keys.
     * @return The value from within the nested Maps.
     */
    private String getNestedValue(Map<String, Object> map, List<String> keys) {
        Object value = map;
        String old_key = "";

        for (String key : keys) {

            //needs to be casted each time because Java does not know that within the Map there are more Maps
            Object new_value = ((Map) value).get(old_key + key);

            //Some keys themselves contain dots again, which previously were used as split points, e.g. there is a key
            //concurrent.phase.time which points to one boolean value, so if no value is found with one key, it is
            // concatenated with the next one on the next round of the loop.
            if (new_value != null) {
                //If that is not the case, simply replace the old Map with the newly found one.
                value = new_value;
            } else {
                if(keys.get(keys.size() - 1).equals(key)){
                    // if the corresponding value can not be found, return the full key.
                    // This is a workaround for, as of now, only environment variables, so it should be fine for the
                    // Documentation but would not be for any actually running agents.
                    return String.join(".", keys);
                } else {
                    old_key = old_key + key + ".";
                }
            }
        }

        return value.toString();
    }

    /**
     * Returns a Duration corresponding to the given value in a String in humanly readable format, e.g. 2h3m20s40ms.
     * Needed for deserializing the YAML using Jackson.
     * @param text String to be parsed.
     * @return Returns the corresponding Duration object.
     */
    private static Duration parseHuman(String text) {
        Matcher m = Pattern.compile("\\s*(?:(\\d+)\\s*(?:hours?|hrs?|h))?" +
                        "\\s*(?:(\\d+)\\s*(?:minutes?|mins?|m))?" +
                        "\\s*(?:(\\d+)\\s*(?:seconds?|secs?|s))?" +
                        "\\s*(?:(\\d+)\\s*(?:milliseconds?|ms))?" +
                        "\\s*", Pattern.CASE_INSENSITIVE)
                .matcher(text);
        if (! m.matches())
            throw new IllegalArgumentException("Not valid duration: " + text);
        int hours = (m.start(1) == -1 ? 0 : Integer.parseInt(m.group(1)));
        int mins  = (m.start(2) == -1 ? 0 : Integer.parseInt(m.group(2)));
        int secs  = (m.start(3) == -1 ? 0 : Integer.parseInt(m.group(3)));
        int ms  = (m.start(4) == -1 ? 0 : Integer.parseInt(m.group(4)));
        return Duration.ofMillis(((hours * 60L + mins) * 60L + secs) * 1000L + ms);
    }

    /**
     * The CustomDurationDeserializer is needed to parse the Duration values in the YAML files using Jackson.
     */
    private static class CustomDurationDeserializer extends JsonDeserializer<Duration> {
        @Override
        public Duration deserialize(JsonParser parser, DeserializationContext context) throws IOException, JsonProcessingException {
            return parseHuman(parser.getText());
        }
    }
}
