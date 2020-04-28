package rocks.inspectit.ocelot.file;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileTestBase {

    protected Path tempDirectory;

    protected void createTestFiles(String... files) {
        try {
            for (String file : files) {
                String path;
                String content;
                if (file.contains("=")) {
                    String[] splitted = file.split("=");
                    path = splitted[0];
                    content = splitted.length == 2 ? splitted[1] : "";
                } else {
                    path = file;
                    content = "";
                }

                Path targetFile = tempDirectory.resolve(path);
                Files.createDirectories(targetFile.getParent());
                Files.write(targetFile, content.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
