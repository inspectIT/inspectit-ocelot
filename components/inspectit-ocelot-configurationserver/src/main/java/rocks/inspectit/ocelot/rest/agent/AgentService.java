package rocks.inspectit.ocelot.rest.agent;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.DeferredResult;
import rocks.inspectit.ocelot.agentcommunication.AgentCommandDispatcher;
import rocks.inspectit.ocelot.agentconfiguration.AgentConfigurationManager;
import rocks.inspectit.ocelot.commons.models.command.impl.EnvironmentCommand;
import rocks.inspectit.ocelot.commons.models.command.impl.LogsCommand;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class AgentService {

    private AgentCommandDispatcher commandDispatcher;

    private InspectitServerSettings configuration;

    public AgentService(AgentCommandDispatcher commandDispatcher, InspectitServerSettings configuration) {
        this.commandDispatcher = commandDispatcher;
        this.configuration = configuration;
    }

    public DeferredResult<ResponseEntity<?>> environment(String agentId) throws ExecutionException {
        EnvironmentCommand environmentCommand = new EnvironmentCommand();
        return commandDispatcher.dispatchCommand(agentId, environmentCommand);
    }

    public DeferredResult<ResponseEntity<?>> logs(String agentId) throws ExecutionException {
        LogsCommand logsCommand = new LogsCommand();
        return commandDispatcher.dispatchCommand(agentId, logsCommand);
    }

    /**
     * Method that combines the current configuration, recent logs and environment details of the given agent
     * into one response for the purpose of downloading it as an archive.
     * This archive is meant to be sent with support requests to help with debugging.
     *
     * @param attributes    - the agent attributes used to select the correct data
     * @param configManager -  the configmanager for retrieving the current configuration
     *
     * @return the deferred result containing the different elements of the support archive
     *
     * @throws ExecutionException - if one of the command executions fails
     */
    public DeferredResult<ResponseEntity<?>> buildSupportArchive(Map<String, String> attributes, AgentConfigurationManager configManager) throws ExecutionException {
        Long responseTimeout = configuration.getAgentCommand().getResponseTimeout().toMillis();
        DeferredResult<ResponseEntity<?>> composedResult = new DeferredResult<ResponseEntity<?>>(responseTimeout) {{
            onTimeout(() -> setErrorResult(ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).build()));
        }};

        SupportArchiveData archiveData = new SupportArchiveData();
        archiveData.currentConfig = configManager.getConfiguration(attributes).getConfigYaml();

        String agentId = attributes.get("agent-id");
        AtomicInteger commandsLeft = new AtomicInteger(0);
        handleCommandResult(environment(agentId), composedResult, commandsLeft, ArchiveElement.ENV, archiveData);
        handleCommandResult(logs(agentId), composedResult, commandsLeft, ArchiveElement.LOG, archiveData);
        return composedResult;
    }

    /**
     * @param command        - the command that was dispatched
     * @param composedResult - the composed result that is being built
     * @param commandsLeft   - the number of commands dispatched and not yet resolved
     * @param element        - enum value that shows for which field the commandResponse is meant
     * @param archiveData    - the archiveData object that is being built
     */
    private void handleCommandResult(DeferredResult<ResponseEntity<?>> command, DeferredResult<ResponseEntity<?>> composedResult, AtomicInteger commandsLeft, ArchiveElement element, SupportArchiveData archiveData) {
        commandsLeft.incrementAndGet();
        command.setResultHandler(result -> {
            if (result instanceof ResponseEntity) {
                int open = commandsLeft.decrementAndGet();
                switch (element) {
                    case ENV:
                        ResponseEntity<EnvironmentCommand.EnvironmentDetail> envResponse = (ResponseEntity<EnvironmentCommand.EnvironmentDetail>) result;
                        archiveData.environmentDetails = envResponse.getBody();
                        break;
                    case LOG:
                        ResponseEntity<String> logResponse = (ResponseEntity<String>) result;
                        archiveData.logs = logResponse.getBody();
                        break;
                }
                if (open == 0) {
                    composedResult.setResult(ResponseEntity.ok().body(archiveData));
                }
            } else {
                composedResult.setResult(ResponseEntity.ok().body(result));
            }
        });
    }

    @Data
    static class SupportArchiveData {

        private String currentConfig;

        private EnvironmentCommand.EnvironmentDetail environmentDetails;

        private String logs;
    }

    private enum ArchiveElement {
        ENV, LOG
    }
}
