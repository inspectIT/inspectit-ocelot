package inspectit.ocelot.configdocsgenerator.parsing;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class used to parse a YAML String into an InspectitConfig object.
 */
@Slf4j
public class ConfigParser {

    /**
     * {@link ObjectMapper} to use for parsing the YAML.
     */
    private final ObjectMapper mapper;

    public ConfigParser() {
        mapper = new ObjectMapper(new YAMLFactory());

        //Add Module to deal with non-standard Duration values in the YAML
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Duration.class, new CustomDurationDeserializer());
        module.addDeserializer(ExporterEnabledState.class, new ExporterEnabledStateDeserializer());
        mapper.registerModule(module);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        //In the YAML property-names are kebab-case in the Java objects CamelCase, Jackson can do that conversion
        //with the following line
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);
    }

    /**
     * Parses YAML describing an InspectitConfig into InspectitConfig object.
     *
     * @param configYaml String in YAML format describing an InspectitConfig.
     *
     * @return InspectitConfig described by given YAML.
     */
    public InspectitConfig parseConfig(String configYaml) throws JsonProcessingException {

        if (!StringUtils.isEmpty(configYaml)) {
            // config YAMLs contain variables that in the inspectit-ocelot-core project are evaluated by Spring-Boot,
            // however here there is no InspectitEnvironment which is used for that, so instead Jackson is used and
            // the variables are evaluated using a custom version of StringSubstitutor.
            Map<String, Object> yamlMap = mapper.readValue(configYaml, Map.class);

            // remove any invalid keys at the top level to make the String parseable for Jackson
            yamlMap.keySet().removeIf(key -> !key.equals("inspectit"));
            String cleanedConfigYaml = mapper.writeValueAsString(yamlMap);

            StringSubstitutor stringSubstitutor = new NestedMapStringSubstitutor(yamlMap);
            cleanedConfigYaml = stringSubstitutor.replace(cleanedConfigYaml);

            //Parse the InspectitConfig from the created YAML String
            ObjectReader reader = mapper.reader().withRootName("inspectit").forType(InspectitConfig.class);
            return reader.readValue(cleanedConfigYaml);
        } else {
            return new InspectitConfig();
        }
    }

    /**
     * The CustomDurationDeserializer is needed to parse the Duration values in the YAML files using Jackson.
     * Normally Jackson would use Duration.parse() for this, however that function expects the Strings in
     * ISO-8601 duration format (e.g. PT20.5S) and in the YAMLs a different format is used (e.g. 50ms).
     */
    private static class CustomDurationDeserializer extends JsonDeserializer<Duration> {

        /**
         * Returns a Duration corresponding to the given value in a String in humanly readable format, e.g. 2h3m20s40ms.
         * Needed for deserializing the YAML using Jackson.
         *
         * @param text String to be parsed.
         *
         * @return Returns the corresponding Duration object.
         */
        private Duration parseHuman(String text) {
            Matcher m = Pattern.compile("\\s*(?:(\\d+)\\s*(?:hours?|hrs?|h))?" + "\\s*(?:(\\d+)\\s*(?:minutes?|mins?|m))?" + "\\s*(?:(\\d+)\\s*(?:seconds?|secs?|s))?" + "\\s*(?:(\\d+)\\s*(?:milliseconds?|ms))?" + "\\s*", Pattern.CASE_INSENSITIVE)
                    .matcher(text);
            if (!m.matches()) {
                throw new IllegalArgumentException("Not valid duration: " + text);
            }
            int hours = (m.start(1) == -1 ? 0 : Integer.parseInt(m.group(1)));
            int mins = (m.start(2) == -1 ? 0 : Integer.parseInt(m.group(2)));
            int secs = (m.start(3) == -1 ? 0 : Integer.parseInt(m.group(3)));
            int ms = (m.start(4) == -1 ? 0 : Integer.parseInt(m.group(4)));
            return Duration.ofMillis(((hours * 60L + mins) * 60L + secs) * 1000L + ms);
        }

        @Override
        public Duration deserialize(JsonParser parser, DeserializationContext context) throws IOException, JsonProcessingException {
            return parseHuman(parser.getText());
        }
    }

    /**
     * The CustomDurationDeserializer is needed to parse 'enabled' fields in exporters using the old Boolean style.
     * Analogous to {@link rocks.inspectit.ocelot.config.conversion.BooleanToExporterEnabledStateConverter}.
     */
    private static class ExporterEnabledStateDeserializer extends JsonDeserializer<ExporterEnabledState> {

        @Override
        public ExporterEnabledState deserialize(JsonParser parser, DeserializationContext context) throws IOException, JsonProcessingException {
            String text = parser.getText().toUpperCase();
            switch (text) {
                case "TRUE":
                    // old setting 'true' is equivalent to IF_CONFIGURED
                    return ExporterEnabledState.IF_CONFIGURED;
                case "FALSE":
                    // old setting 'false' is equivalent to DISABLED
                    return ExporterEnabledState.DISABLED;
                default:
                    return ExporterEnabledState.valueOf(text);
            }
        }
    }
}
