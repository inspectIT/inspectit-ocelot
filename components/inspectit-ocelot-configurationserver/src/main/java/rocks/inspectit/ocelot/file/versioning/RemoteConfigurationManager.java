package rocks.inspectit.ocelot.file.versioning;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder;
import org.eclipse.jgit.util.FS;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.config.model.RemoteRepositorySettings;
import rocks.inspectit.ocelot.config.model.RemoteRepositorySettings.AuthenticationType;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Encapsulating the logic for interacting with the remote configuration repository.
 */
@Slf4j
@AllArgsConstructor
public class RemoteConfigurationManager {

    private static final String SSH_DIR = ".ssh";

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

        RemoteRepositorySettings sourceRepository = settings.getRemoteConfigurations().getPullRepository();
        if (sourceRepository != null) {
            updateRemoteRef(sourceRepository);
        }

        RemoteRepositorySettings targetRepository = settings.getRemoteConfigurations().getPushRepository();
        if (targetRepository != null) {
            updateRemoteRef(targetRepository);
        }
    }

    /**
     * Adds or updates a remote ref for the given {@link RemoteRepositorySettings}.
     *
     * @param repositorySettings the settings representing the ref to add
     */
    private void updateRemoteRef(RemoteRepositorySettings repositorySettings) throws GitAPIException {
        if (!hasConfigurationRemote(repositorySettings)) {
            log.info("Remote ref '{}' for remote configurations does not exists in the local Git, so it will be added.", repositorySettings
                    .getRemoteName());
            git.remoteAdd()
                    .setName(repositorySettings.getRemoteName())
                    .setUri(repositorySettings.getGitRepositoryUri())
                    .call();
        } else {
            log.debug("Remote ref '{}' for remote configurations exists in the local Git and will be updated.", repositorySettings
                    .getRemoteName());
            git.remoteSetUrl()
                    .setRemoteName(repositorySettings.getRemoteName())
                    .setRemoteUri(repositorySettings.getGitRepositoryUri())
                    .call();
        }
    }

    /**
     * @return Returns whether the remote ref for the configuration remote does already exist.
     */
    private boolean hasConfigurationRemote(RemoteRepositorySettings repositorySettings) {
        try {
            List<RemoteConfig> remotes = git.remoteList().call();
            return remotes.stream().anyMatch(remote -> remote.getName().equals(repositorySettings.getRemoteName()));
        } catch (GitAPIException e) {
            return false;
        }
    }

    /**
     * Pushes a specific local branch to the configured configuration remote ref using the specified branch name. Here
     * the first push attempt is not forced. In case the push attempt is rejected because it cannot be performed
     * fast-forward, it will be forced if configured.
     *
     * @param localBranch      the local branch to push
     * @param targetRepository the settings for the repository to push to
     *
     * @return the result of the push command
     */
    public RemoteRefUpdate.Status pushBranch(Branch localBranch, RemoteRepositorySettings targetRepository) throws GitAPIException {
        RemoteRefUpdate.Status pushStatus = push(localBranch, targetRepository, false);

        if (pushStatus == RemoteRefUpdate.Status.REJECTED_NONFASTFORWARD && targetRepository.isUseForcePush()) {
            log.warn("Fast-Forward push was rejected. Pushing the commit will be forced, now!");
            pushStatus = push(localBranch, targetRepository, true);
        }

        return pushStatus;
    }

    /**
     * Pushes a specific local branch to the configured configuration remote ref using the specified branch name.
     *
     * @param localBranch      the local branch to push
     * @param targetRepository the settings for the repository to push to
     * @param useForcePush     whether the push command should be forced
     *
     * @return the result status of the push command
     */
    private RemoteRefUpdate.Status push(Branch localBranch, RemoteRepositorySettings targetRepository, boolean useForcePush) throws GitAPIException {
        if (targetRepository == null) {
            throw new IllegalArgumentException("The repository settings must not be null.");
        }

        RefSpec refSpec = new RefSpec(localBranch.getBranchName() + ":refs/heads/" + targetRepository.getBranchName());

        log.info("Pushing to remote '{}' [{}] with refspec '{}'. Using force-push: {}", targetRepository.getRemoteName(), targetRepository
                .getGitRepositoryUri(), refSpec.toString(), useForcePush);

        PushCommand pushCommand = git.push()
                .setRemote(targetRepository.getRemoteName())
                .setRefSpecs(refSpec)
                .setForce(useForcePush);
        authenticateCommand(pushCommand, targetRepository);

        Iterable<PushResult> pushResults = pushCommand.call();
        PushResult pushResult = pushResults.iterator().next();

        if (pushResult == null) {
            log.warn("Pushing of local branch '{}' may have failed. No push-result available.", localBranch);
            return null;
        } else {
            RemoteRefUpdate remoteUpdate = pushResult.getRemoteUpdate("refs/heads/" + targetRepository.getBranchName());
            RemoteRefUpdate.Status status = remoteUpdate.getStatus();

            if (status == RemoteRefUpdate.Status.OK || status == RemoteRefUpdate.Status.UP_TO_DATE) {
                log.info("Pushing to remote repository '{}' was successful: {}", targetRepository.getRemoteName(), status);
            } else {
                log.error("Pushing to remote repository '{}' has been failed: {}", targetRepository.getRemoteName(), status);
            }
            return status;
        }
    }

    /**
     * Fetches the branch represented by the given {@link RemoteRepositorySettings}.
     *
     * @param sourceRepository the definition of the remote repository and branch
     */
    public void fetchSourceBranch(RemoteRepositorySettings sourceRepository) throws GitAPIException {
        log.info("Fetching branch '{}' from configuration remote '{}'.", sourceRepository.getBranchName(), sourceRepository
                .getRemoteName());

        if (!sourceBranchExistsOnRemote(sourceRepository)) {
            throw new IllegalStateException(String.format("Specified configuration source branch '%s' does not exists on remote '%s'.", sourceRepository
                    .getBranchName(), sourceRepository.getRemoteName()));
        }

        FetchCommand fetchCommand = git.fetch()
                .setRemote(sourceRepository.getRemoteName())
                .setRefSpecs("refs/heads/" + sourceRepository.getBranchName() + ":refs/heads/" + sourceRepository.getBranchName());
        authenticateCommand(fetchCommand, sourceRepository);

        FetchResult fetchResult = fetchCommand.call();

        if (fetchResult.getTrackingRefUpdates().isEmpty()) {
            log.info("No change has been fetched.");
        } else {
            for (TrackingRefUpdate refUpdate : fetchResult.getTrackingRefUpdates()) {
                log.info("Fetching from '{}' ended with result: {}", refUpdate.getRemoteName(), refUpdate.getResult());
            }
        }
    }

    /**
     * Checks whether the branch represented by the given {@link RemoteRepositorySettings} exists on the remote repository.
     *
     * @param sourceRepository the definition of the remote repository and branch
     *
     * @return {@code true} if the remote repository has the branch or {@code false} otherwise
     */
    public boolean sourceBranchExistsOnRemote(RemoteRepositorySettings sourceRepository) throws GitAPIException {
        LsRemoteCommand lsRemoteCommand = git.lsRemote().setRemote(sourceRepository.getRemoteName());
        authenticateCommand(lsRemoteCommand, sourceRepository);

        Collection<Ref> refs = lsRemoteCommand.call();

        Optional<Ref> sourceBranch = refs.stream()
                .filter(ref -> ref.getName().equals("refs/heads/" + sourceRepository.getBranchName()))
                .findAny();

        return sourceBranch.isPresent();
    }

    /**
     * Injects a {@link CredentialsProvider} for executing a user-password authentication. This is used for HTTP(s)-remotes.
     *
     * @param command          the command to authenticate
     * @param remoteRepository the settings for the repository
     */
    private void authenticatePassword(TransportCommand<?, ?> command, RemoteRepositorySettings remoteRepository) {
        String username = remoteRepository.getUsername();
        String password = remoteRepository.getPassword();
        UsernamePasswordCredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(username, password);
        command.setCredentialsProvider(credentialsProvider);
    }

    /**
     * Injects a session factory for creating SSH sessions. This allows the usage of private keys for connection authentication.
     *
     * @param command          the command to authenticate
     * @param remoteRepository the settings for the repository
     */
    private void authenticatePpk(TransportCommand<?, ?> command, RemoteRepositorySettings remoteRepository) {
        SshdSessionFactoryBuilder sshSessionFactoryBuilder = createSshSessionFactoryBuilder();

        String privateKeyFile = remoteRepository.getPrivateKeyFile();
        if(StringUtils.isNotBlank(privateKeyFile)) {
            // specify a specific private key, of configured -> no defaults from .ssh
            sshSessionFactoryBuilder.setDefaultIdentities(file -> Collections.singletonList(Paths.get(privateKeyFile)));
        }

        command.setTransportConfigCallback(transport -> {
            SshTransport sshTransport = (SshTransport) transport;
            sshTransport.setSshSessionFactory(sshSessionFactoryBuilder.build(null));
        });
    }

    /**
     * Creates a SshSessionFactoryBuilder with user home and ssh directory.
     *
     * @return The created builder
     */
    private SshdSessionFactoryBuilder createSshSessionFactoryBuilder() {
        SshdSessionFactoryBuilder sshSessionFactoryBuilder = new SshdSessionFactoryBuilder();

        sshSessionFactoryBuilder.setHomeDirectory(FS.DETECTED.userHome());
        sshSessionFactoryBuilder.setSshDirectory(new File(FS.DETECTED.userHome(), "/" + SSH_DIR));

        return sshSessionFactoryBuilder;
    }

    /**
     * Authenticates the given command with a password or PPK authentication, depending on the given {@link RemoteRepositorySettings}.
     *
     * @param command          the command to authenticate
     * @param remoteRepository the settings for the repository
     */
    private void authenticateCommand(TransportCommand<?, ?> command, RemoteRepositorySettings remoteRepository) {
        if (remoteRepository.getAuthenticationType() == AuthenticationType.PASSWORD) {
            authenticatePassword(command, remoteRepository);
        } else if (remoteRepository.getAuthenticationType() == AuthenticationType.PPK) {
            authenticatePpk(command, remoteRepository);
        }
    }
}
