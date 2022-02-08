package inspectit.ocelot.config.doc.generator;

import inspectit.ocelot.config.doc.generator.docobjects.ConfigDocumentation;
import inspectit.ocelot.config.doc.generator.docobjects.DocObjectGenerator;
import inspectit.ocelot.config.doc.generator.parsing.ConfigParser;
import rocks.inspectit.ocelot.config.model.InspectitConfig;

public class ConfigDocManager {

    public ConfigDocumentation loadConfigDocumentation(String configYaml){
        ConfigParser configParser = new ConfigParser();
        InspectitConfig config = configParser.parseConfig(configYaml);

        DocObjectGenerator docObjectGenerator = new DocObjectGenerator();
        return docObjectGenerator.generateConfigDocumentation(config);
    }

}
