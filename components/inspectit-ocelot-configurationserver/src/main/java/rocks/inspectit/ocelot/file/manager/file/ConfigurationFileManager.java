package rocks.inspectit.ocelot.file.manager.file;

import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class ConfigurationFileManager extends AbstractFileManager {

    private static final String basePath = FILES_DIRECTORY + File.separator + CONFIG_DIRECTORY + File.separator;

    @Override
    protected String resolvePath(String path) {
        return basePath + path;
    }
}
