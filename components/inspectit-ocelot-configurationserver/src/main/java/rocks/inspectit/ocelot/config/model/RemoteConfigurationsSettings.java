package rocks.inspectit.ocelot.config.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.transport.URIish;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import javax.validation.constraints.AssertFalse;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;
import java.net.URI;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RemoteConfigurationsSettings {

    @Builder.Default
    private boolean enabled = false;

    private String remoteName;

    private URIish gitRepositoryUri;

    private String sourceBranch;

    private String targetBranch;

    @Builder.Default
    @NotNull
    private AuthenticationType authenticationType = AuthenticationType.NONE;

    private String username;

    private String password;

    private String privateKeyFile;

    public enum AuthenticationType {
        NONE, PASSWORD, PPK;
    }

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
