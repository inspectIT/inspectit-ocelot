package rocks.inspectit.ocelot.file.versioning;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.util.FS;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.config.model.RemoteConfigurationsSettings;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Encapsulating the logic for interacting with the remote configuration repository.
 */
@Slf4j
@AllArgsConstructor
public class RemoteConfigurationManager {

    /**
     * The current server settings.
     */
    private final InspectitServerSettings settings;

    /**
     * The Git instance of the working directory.
     */
    private final Git git;

    /**
     * Updating the remote ref of the configuration repository in the current working directory Git. The remote ref is
     * created or, in case it already exists, updated.
     */
    public void updateRemoteRefs() throws GitAPIException {
        RemoteConfigurationsSettings remoteSettings = settings.getRemoteConfigurations();
        String remoteName = remoteSettings.getRemoteName();

        if (!hasConfigurationRemote()) {
            log.info("No configuration remote repository is configured for the local Git repository, thus, adding '{}'.", remoteName);
            git.remoteAdd()
                    .setName(remoteSettings.getRemoteName())
                    .setUri(remoteSettings.getGitRepositoryUriAsUriisch())
                    .call();
        } else {
            log.debug("Remote '{}' for remote configurations exists and will be updated.", remoteName);
            git.remoteSetUrl()
                    .setRemoteName(remoteSettings.getRemoteName())
                    .setRemoteUri(remoteSettings.getGitRepositoryUriAsUriisch())
                    .call();
        }
    }

    /**
     * @return Returns whether the remote ref for the configuration remote does already exist.
     */
    private boolean hasConfigurationRemote() {
        String remoteName = settings.getRemoteConfigurations().getRemoteName();
        try {
            List<RemoteConfig> remotes = git.remoteList().call();
            return remotes.stream().anyMatch(remote -> remote.getName().equals(remoteName));
        } catch (GitAPIException e) {
            return false;
        }
    }

    /**
     * Pushes a specific local branch to the configured configuration remote ref using the specified branch name.
     *
     * @param localBranch      the local branch to push
     * @param remoteBranchName the name of the branch on the remote
     */
    public void pushBranch(Branch localBranch, String remoteBranchName) throws GitAPIException {
        RemoteConfigurationsSettings remoteSettings = settings.getRemoteConfigurations();
        String remoteName = remoteSettings.getRemoteName();

        RefSpec refSpec = new RefSpec(localBranch.getBranchName() + ":refs/heads/" + remoteBranchName);

        PushCommand pushCommand = git.push().setRemote(remoteName).setRefSpecs(refSpec);

        if (remoteSettings.getAuthenticationType() == RemoteConfigurationsSettings.AuthenticationType.PASSWORD) {
            authenticatePassword(pushCommand);
        } else if (remoteSettings.getAuthenticationType() == RemoteConfigurationsSettings.AuthenticationType.PPK) {
            authenticatePpk(pushCommand);
        }

        Iterable<PushResult> pushResults = pushCommand.call();
        PushResult pushResult = pushResults.iterator().next();

        if (pushResult == null) {
            log.warn("Pushing of localBranch {} may have failed. No push-result available.", localBranch);
        } else {
            RemoteRefUpdate remoteUpdate = pushResult.getRemoteUpdate("refs/heads/" + remoteBranchName);
            RemoteRefUpdate.Status status = remoteUpdate.getStatus();

            if (status == RemoteRefUpdate.Status.OK || status == RemoteRefUpdate.Status.UP_TO_DATE) {
                log.info("Pushing to remote repository was successful: {}", status);
            } else {
                log.error("Pushing to remote repository has been failed: {}", status);
            }
        }
    }

    /**
     * Injects a {@link CredentialsProvider} for executing a user-password authentication. This is used for HTTP(s)-remotes.
     *
     * @param push the push command to authenticate
     */
    private void authenticatePassword(PushCommand push) {
        String username = settings.getRemoteConfigurations().getUsername();
        String password = settings.getRemoteConfigurations().getPassword();
        UsernamePasswordCredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(username, password);
        push.setCredentialsProvider(credentialsProvider);
    }

    /**
     * Injects a session factory for creating SSH sessions. This allows the usage of private keys for connection authentication.
     *
     * @param push the push command to authenticate
     */
    private void authenticatePpk(PushCommand push) {
        SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
            @Override
            protected JSch createDefaultJSch(FS fs) throws JSchException {
                JSch defaultJSch = super.createDefaultJSch(fs);

                String privateKeyFile = settings.getRemoteConfigurations().getPrivateKeyFile();
                if (StringUtils.isNotBlank(privateKeyFile)) {
                    defaultJSch.addIdentity(privateKeyFile);
                }

                return defaultJSch;
            }

            @Override
            protected void configure(OpenSshConfig.Host hc, Session session) {
            }
        };

        push.setTransportConfigCallback(transport -> {
            SshTransport sshTransport = (SshTransport) transport;
            sshTransport.setSshSessionFactory(sshSessionFactory);
        });
    }
}
