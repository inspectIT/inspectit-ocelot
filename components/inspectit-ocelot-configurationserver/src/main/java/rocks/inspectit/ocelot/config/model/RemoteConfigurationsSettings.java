package rocks.inspectit.ocelot.config.model;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Settings for connecting the configuration server to remote Git repositories.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RemoteConfigurationsSettings {

    /**
     * Whether remote Git repositories should be used for configuration management.
     */
    @Builder.Default
    private boolean enabled = false;

    /**
     * Whether the current live branch should be pushed during startup.
     */
    @Builder.Default
    private boolean pushAtStartup = false;

    /**
     * Whether the remote source branch should be fetched and merged into the current workspace branch during startup.
     */
    @Builder.Default
    private boolean pullAtStartup = false;

    /**
     * Defines whether the configuration files of the configuration source repository should be pulled on the initial
     * configuration synchronization. The initial synchronization is not related to the {@link #pullAtStartup} property!
     */
    @Builder.Default
    private boolean initialConfigurationSync = false;

    /**
     * Whether synchronized files should be promoted automatically, after they have been fetched from the configuration
     * remote.
     */
    @Builder.Default
    private boolean autoPromotion = true;

    /**
     * The remote Git repository which will be used to fetch workspace-configurations from.
     */
    @Valid
    private RemoteRepositorySettings pullRepository;

    /**
     * The remote Git repository which will be used to push live-configurations to.
     */
    @Valid
    private RemoteRepositorySettings pushRepository;
}
