package rocks.inspectit.oce.eum.server.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import rocks.inspectit.oce.eum.server.file.FileData;
import rocks.inspectit.oce.eum.server.service.ConfigurationFileService;
import rocks.inspectit.oce.eum.server.service.RestartService;

import java.io.IOException;

/**
 * Rest Controller for all operations concerning the configuration file of the EUM-Server.
 */
@RestController()
@RequestMapping("/configuration/")
@Slf4j
public class ConfigurationFileController {

    @Autowired
    private RestartService restartService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ConfigurationFileService configFileService;

    /**
     * Returns the current configuration file's content as String.
     * @return The current configuration file's content as String.
     */
    @GetMapping("file")
    public String getConfigurationFile() {
        return configFileService.getFile();
    }

    /**
     * Returns the default configuration as String.
     * @return The default configuration as String.
     */
    @GetMapping("default")
    public String getDefaultConfigFile() {
        return configFileService.getDefaultConfig();
    }

    /**
     * Expects a json in the format {content: **MY-FILE-CONTENT**}. The String found in the content-key will be saved
     * in the configuration file of the server.
     * @param content A json in the format {content: **MY-FILE-CONTENT**} in which the value of the content-key is a
     *                String resembling the new content of the configuration file.
     */
    @PostMapping("file")
    public void saveConfigurationFile(@RequestBody String content) throws IOException {
        String fileContent;
        if (content == null) {
            fileContent = "";
        } else {
            FileData data = objectMapper.readValue(content, FileData.class);
            fileContent = data.getContent();
        }
        configFileService.saveFile(fileContent);
    }

    /**
     * Applies the current config-file. This is done by restarting the Server with the {@link RestartService}.
     */
    @PostMapping("apply")
    public void applyConfiguration() throws IOException, InterruptedException {
        restartService.restartApplication(null);
    }

}
