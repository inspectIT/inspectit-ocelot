package rocks.inspectit.ocelot.config.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.jgit.transport.URIish;
import rocks.inspectit.ocelot.config.validation.RemoteRepositorySettingsConstraint;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@RemoteRepositorySettingsConstraint
public class RemoteRepositorySettings {

    /**
     * Supported authentication types.
     */
    public enum AuthenticationType {
        NONE, PASSWORD, PPK;
    }

    /**
     * The name of the remote ref in the local Git repository.
     */
    @NotBlank
    private String remoteName;

    /**
     * The URI to the remote Git repository.
     */
    @NotNull
    private URIish gitRepositoryUri;

    /**
     * The branch name on the remote Git repository which will be used (e.g. to fetch workspace-configurations from or
     * to push the live-configurations to).
     */
    @NotBlank
    private String branchName;

    /**
     * Whether force push should used for pushing to this remote.
     */
    @Builder.Default
    private boolean useForcePush = false;

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
}
