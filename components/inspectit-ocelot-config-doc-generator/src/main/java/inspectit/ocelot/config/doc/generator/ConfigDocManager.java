package inspectit.ocelot.config.doc.generator;

import inspectit.ocelot.config.doc.generator.docobjects.ConfigDocumentation;
import inspectit.ocelot.config.doc.generator.docobjects.DocObjectGenerator;
import rocks.inspectit.ocelot.config.model.InspectitConfig;

public class ConfigDocManager {

    public ConfigDocumentation getConfigDocumentation(String configYaml){
        ConfigParser configParser = new ConfigParser();
        InspectitConfig config = configParser.parseConfig(configYaml);

        DocObjectGenerator docObjectGenerator = new DocObjectGenerator();
        return docObjectGenerator.generateFullDocObject(config);
    }

}
