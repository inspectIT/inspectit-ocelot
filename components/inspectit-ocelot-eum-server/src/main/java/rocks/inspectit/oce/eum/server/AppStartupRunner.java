package rocks.inspectit.oce.eum.server;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

@Slf4j
@Component
public class AppStartupRunner implements ApplicationRunner {

    /**
     * The file used to load the servers's version.
     */
    private static final String SERVER_VERSION_INFORMATION_FILE = "/eum-version.info";

    private static final String UNKNOWN = "UNKNOWN";

    /**
     * The server's version.
     */
    private String serverVersion;

    /**
     * The date the server was built.
     */
    private String serverBuildDate;

    /**
     * The Boomerangjs version shipped with the server.
     */
    private String bommerangjsVersion;

    @Override
    public void run(ApplicationArguments args) {
        readVersionInformation();

        log.info("> Version Information");
        log.info("\tVersion:             {}", serverVersion);
        log.info("\tBuild Date:          {}", serverBuildDate);
        log.info("\tBoomerangjs Version: {}", bommerangjsVersion);
    }

    /**
     * Loads the agent's version information from the {@link #SERVER_VERSION_INFORMATION_FILE} file.
     */
    private void readVersionInformation() {
        try (InputStream inputStream = getClass().getResourceAsStream(SERVER_VERSION_INFORMATION_FILE)) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            serverVersion = reader.readLine();
            serverBuildDate = reader.readLine();
            bommerangjsVersion = reader.readLine();
        } catch (Exception e) {
            log.warn("Could not read server version information file.");
            serverVersion = UNKNOWN;
            serverBuildDate = UNKNOWN;
            bommerangjsVersion = UNKNOWN;
        }
    }
}