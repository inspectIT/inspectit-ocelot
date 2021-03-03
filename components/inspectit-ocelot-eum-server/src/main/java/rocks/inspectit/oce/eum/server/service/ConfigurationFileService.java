package rocks.inspectit.oce.eum.server.service;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

/**
 * This class contains all methods concerning the editing of the configuration file by the server's configuration UI.
 *
 */
@Component
public class ConfigurationFileService {

    /**
     * File encoding to be used by all read and write accesses.
     */
    static final String FILE_ENCODING = "UTF-8";

    /**
     * Default file path which is used when no file path was given at server startup.
     */
    static final String DEFAULT_FILE_PATH = "./application.yml";

    /**
     * Actual file path used. If a path is present at spring.config.location, this path is used.
     * Otherwise DEFAULT_FILE_PATH is used.
     */
    String filePath;

    /**
     * This method is called upon server start. If spring.config.location is not set and there is no configuration file
     * present in the DEFAULT_FILE_PATH, the configuration file is created with the default config as it's content.
     * Also sets the filePath variable.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void init() throws IOException {
        String pathProperty = System.getProperty("spring.config.location");
        if(pathProperty != null) {
            filePath = pathProperty.substring("file:".length());
        } else {
            filePath = DEFAULT_FILE_PATH;
        }
        File defaultFile = new File(filePath);
        if(defaultFile.createNewFile()) {
            saveFile(getDefaultConfig());
        }
    }

    /**
     * Takes a String resembling a file content and writes it to the file present in the file path with which the class
     * was initiated.
     * @param content A String resembling the content to be written.
     */
    public void saveFile(String content) throws IOException {
        Path path = Paths.get(filePath);
        Files.write(path, content.getBytes(FILE_ENCODING));
    }

    /**
     * Returns a String resembling the content of the file present in the file path with which the class was initiated.
     * @return A String resembling the content of the file.
     */
    public String getFile(){
        Scanner fileScanner = null;
        StringBuilder config = new StringBuilder();
        
        try {
            File fileObject = new File(filePath);
            fileScanner = new Scanner(fileObject);
            while (fileScanner.hasNextLine()) {
                config.append(fileScanner.nextLine()).append("\n");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if(fileScanner != null) {

                fileScanner.close();
            }
        }
            

        return config.toString();
    }

    /**
     * Returns a String resembling the content of the application.yml file present in the resource-folder of the
     * server.
     * @return A String resembling the content of the application.yml present in the resource-folder of the server.
     */
    public String getDefaultConfig() {
        StringBuilder config = new StringBuilder();

        InputStream in = getClass().getResourceAsStream("/application.yml");
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        try {
            for(String line = reader.readLine(); line != null; line = reader.readLine()) {
                config.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return config.toString();
    }

}
