package rocks.inspectit.ocelot.config.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.transport.URIish;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import rocks.inspectit.ocelot.config.validation.RemoteConfigurationsConstraint;

import javax.validation.constraints.AssertFalse;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.net.URISyntaxException;

/**
 * Settings for connecting the configuration server to remote Git repositories.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@RemoteConfigurationsConstraint
public class RemoteConfigurationsSettings {

    /**
     * Supported authentication types.
     */
    public enum AuthenticationType {
        NONE, PASSWORD, PPK;
    }

    /**
     * Whether remote Git repositories should be used for configuration management.
     */
    @Builder.Default
    private boolean enabled = false;

    /**
     * The name of the remote ref in the local Git repository.
     */
    @NotBlank
    private String remoteName;

    /**
     * The URI to the remote Git repository.
     */
    private String gitRepositoryUri;

    /**
     * The branch name on the remote Git repository which will be used to fetch workspace-configurations from.
     */
    @NotBlank
    private String sourceBranch;

    /**
     * The branch name on the remote Git repository which will be used to push live-configurations to.
     */
    @NotBlank
    private String targetBranch;

    /**
     * The type of authentication to use.
     */
    @Builder.Default
    @NotNull
    private AuthenticationType authenticationType = AuthenticationType.NONE;

    /**
     * The username for accessing the remote repository. Only used in case of PASSWORD authentication.
     */
    private String username;

    /**
     * The password for accessing the remote repository. Only used in case of PASSWORD authentication.
     */
    private String password;

    /**
     * The private key to use for SSH authentication. Only used in case of PPK authentication.
     */
    private String privateKeyFile;

    /**
     * @return Returns the repository URI as an {@link URIish}.
     */
    public URIish getGitRepositoryUriAsUriisch() {
        try {
            return new URIish(gitRepositoryUri);
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
