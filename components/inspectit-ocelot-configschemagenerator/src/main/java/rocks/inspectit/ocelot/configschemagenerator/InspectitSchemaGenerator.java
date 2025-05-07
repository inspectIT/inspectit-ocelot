package rocks.inspectit.ocelot.configschemagenerator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.github.victools.jsonschema.generator.*;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.javax.validation.JavaxValidationModule;
import com.github.victools.jsonschema.module.javax.validation.JavaxValidationOption;
import rocks.inspectit.ocelot.config.model.InspectitConfig;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * This class is only used by the Gradle task {@code generateConfigSchema} to create a JSON schema file
 * for {@link InspectitConfig}. The schema file supports developers when writing an inspectIT configuration
 * in their IDE. Even though the schema uses JSON, it can be used for both JSON and YAML configurations.
 */
public class InspectitSchemaGenerator {
    private static final Logger log = Logger.getLogger(InspectitSchemaGenerator.class.getName());

    /**
     * Generates JSON schema and creates a file at the specified path.
     *
     * @param args should contain file path for the schema
     */
    public static void main(String[] args) {
        if(args.length < 1)
            throw new RuntimeException("Output path has to be specified!");

        JsonNode configSchema = generateSchema();

        try {
            String outputPath = args[0];
            writeSchemaToFile(configSchema, outputPath);
        } catch (Exception e) {
            log.severe("Could not generate schema file: " + e.getMessage());
        }

        log.info("inspectIT Ocelot configuration schema has been generated");
    }

    /**
     * Generates the JSON schema for {@link InspectitConfigSchema}.
     * The camelCase of the Java property names are converted to kebab-case,
     * which is also used in the file editor of the configuration server.
     *
     * @return the generated schema
     */
    static JsonNode generateSchema() {
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_7, OptionPreset.PLAIN_JSON);

        PropertyNamingStrategies.KebabCaseStrategy strategy = PropertyNamingStrategies.KebabCaseStrategy.INSTANCE;
        configBuilder.forFields().withPropertyNameOverrideResolver(field -> strategy.translate(field.getName()));

        SchemaGeneratorConfig generatorConfig = configBuilder
                .with(new JacksonModule())
                .with(new JavaxValidationModule(JavaxValidationOption.INCLUDE_PATTERN_EXPRESSIONS))
                .with(Option.MAP_VALUES_AS_ADDITIONAL_PROPERTIES)
                .build();

        SchemaGenerator generator = new SchemaGenerator(generatorConfig);
        return generator.generateSchema(InspectitConfigSchema.class);
    }

    /**
     * Writes the provided schema into a file.
     *
     * @param schema the json schema
     * @param filePath the path to create the schema file
     */
    private static void writeSchemaToFile(JsonNode schema, String filePath) throws IOException {
        File outputFile = new File(filePath);
        outputFile.getParentFile().mkdirs();

        ObjectMapper mapper = new ObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, schema);
    }
}
