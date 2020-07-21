package rocks.inspectit.ocelot.config.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import java.time.Duration;

/**
 * Configuration object allowing relaxed configuration binding via spring.
 */
@Data
@ConfigurationProperties("inspectit-config-server")
@Configuration
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Validated
public class InspectitServerSettings {

    /**
     * The directory in which the server stores all its files.
     */
    private String workingDirectory;

    /**
     * The mail suffix used for internal users.
     */
    private String mailSuffix;

    /**
     * The duration until an authentication token generated via /api/v1/account/token is valid.
     * After the token has expired, a new one has to be acquired.
     */
    private Duration tokenLifespan;

    /**
     * The default user to create if no user database is found.
     */
    private DefaultUserSettings defaultUser;

    /**
     * The number of threads to use for asynchronous tasks.
     */
    private int threadPoolSize;

    /**
     * The estimated upper bound of agents who connect to this server.
     * This is only used to limit internal caches of the server and not as hard limitation.
     */
    private int maxAgents;

    /**
     * After the given amount of inactivity from an agent, it is evicted from all caches.
     */
    private Duration agentEvictionDelay;

    @Builder.Default
    private KapacitorSettings kapacitor = new KapacitorSettings();

    /**
     * The security settings of the configuration server.
     */
    @Valid
    @Builder.Default
    private SecuritySettings security = SecuritySettings.builder().build();
}
