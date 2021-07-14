package rocks.inspectit.ocelot.config.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.transport.URIish;

import javax.validation.constraints.AssertFalse;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;

/**
 * Settings for connecting the configuration server to remote Git repositories.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
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
    private String remoteName;

    /**
     * The URI to the remote Git repository.
     */
    private URIish gitRepositoryUri;

    /**
     * The branch name on the remote Git repository which will be used to fetch workspace-configurations from.
     */
    private String sourceBranch;

    /**
     * The branch name on the remote Git repository which will be used to push live-configurations to.
     */
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

    @AssertFalse
    public boolean isRemoteNameBlank() {
        return enabled && StringUtils.isBlank(remoteName);
    }

    @AssertTrue(message = "The URI of the remote Git must be specified!")
    public boolean isGitRepositoryUriNotNull() {
        return !enabled || gitRepositoryUri != null;
    }

    @AssertFalse(message = "Authentication method 'PPK' can only be used with an SSH connection.")
    public boolean isHttpWithPpk() {
        if (!enabled) {
            return false;
        }

        String scheme = gitRepositoryUri.getScheme();
        return !(scheme == null || scheme.equals("ssh")) && authenticationType == AuthenticationType.PPK;
    }

    @AssertFalse(message = "SSH using password authentication is not supported.")
    public boolean isSshWithPassword() {
        if (!enabled) {
            return false;
        }

        String scheme = gitRepositoryUri.getScheme();
        return (scheme == null || scheme.equals("ssh")) && authenticationType == AuthenticationType.PASSWORD;
    }
}
