package inspectit.ocelot.configdocsgenerator.parsing;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.YamlProcessor;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.PropertySourcesPlaceholdersResolver;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileUrlResource;
import rocks.inspectit.ocelot.config.conversion.InspectitConfigConversionService;
import rocks.inspectit.ocelot.config.model.InspectitConfig;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Class used to parse a YAML String into an InspectitConfig object.
 */
@Slf4j
public class ConfigParser {

    /**
     * Parses YAML describing an InspectitConfig into InspectitConfig object.
     *
     * @param configYaml String in YAML format describing an InspectitConfig.
     *
     * @return InspectitConfig described by given YAML.
     */
    public InspectitConfig parseConfig(String configYaml) throws IOException {

        if (!StringUtils.isEmpty(configYaml)) {

            File tempFile = File.createTempFile("temp-", ".tmp");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
                writer.write(configYaml);
            }

            // Read yaml into Properties
            YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
            factory.setResources(new FileUrlResource(tempFile.getAbsolutePath()));
            factory.setDocumentMatchers((profile) -> YamlProcessor.MatchStatus.FOUND);
            factory.afterPropertiesSet();
            Properties properties = factory.getObject();

            // Create ConfigurationPropertySource from Properties
            ConfigurationPropertySource propertySource = new MapConfigurationPropertySource(properties);
            // Add to list because Binder expects Iterable<ConfigurationPropertySource>
            List<ConfigurationPropertySource> configurationPropertySources = new ArrayList<>();
            configurationPropertySources.add(propertySource);

            // Read yaml into PropertySources to create PropertySourcesPlaceholdersResolver
            List<PropertySource<?>> propertySources = new YamlPropertySourceLoader().load("tempDocsConfig", new FileUrlResource(tempFile.getAbsolutePath()));

            boolean success = tempFile.delete();
            if (!success) {
                log.warn("Could not delete temp file '{}' used to generate ConfigDocs.", tempFile.getAbsolutePath());
            } else {
                log.debug("Successfully deleted temp file '{}' used to generate ConfigDocs.", tempFile.getAbsolutePath());
            }

            Binder binder = new Binder(configurationPropertySources, new PropertySourcesPlaceholdersResolver(propertySources), InspectitConfigConversionService.getInstance());

            // Create InspectitConfig from ConfigurationPropertySource
            return binder.bind("inspectit", InspectitConfig.class).get();
        } else {
            return new InspectitConfig();
        }
    }
}
