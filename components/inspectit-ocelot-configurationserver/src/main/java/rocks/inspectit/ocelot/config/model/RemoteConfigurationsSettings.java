package rocks.inspectit.ocelot.config.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.jgit.transport.URIish;

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

    @AssertTrue(message = "The URI of the remote Git must be specified!")
    public boolean isGitRepositoryUriNotNull() {
        return !enabled || gitRepositoryUri != null;
    }

    @AssertTrue
    public boolean isValidAuthentication() {
        if (!enabled) {
            return true;
        }
        String scheme = gitRepositoryUri.getScheme();
        if (scheme == null || scheme.equals("ssh")) {
            return true;
        } else {
            return authenticationType != AuthenticationType.PPK;
        }
    }

    @AssertFalse(message = "SSH using password authentication is not supported.")
    public boolean isSshWithPassword() {
        if (!enabled) {
            return true;
        }
        
        String scheme = gitRepositoryUri.getScheme();
        return (scheme == null || scheme.equals("ssh")) && authenticationType == AuthenticationType.PASSWORD;
    }
}
