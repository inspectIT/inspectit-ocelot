package rocks.inspectit.ocelot.rest.agentstatus;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rocks.inspectit.ocelot.agentstatus.AgentStatus;
import rocks.inspectit.ocelot.agentstatus.AgentStatusManager;
import rocks.inspectit.ocelot.rest.AbstractBaseController;
import rocks.inspectit.ocelot.security.config.UserRoleConfiguration;

import java.util.Collection;
import java.util.Map;


/**
 * The rest controller providing the interface used by the agent for configuration fetching.
 */
@RestController
public class AgentStatusController extends AbstractBaseController {

    @Autowired
    private AgentStatusManager statusManager;

    @Operation(summary = "Fetch the List of Agent Statuses", description = "Gives a list of connected agents")
    @GetMapping(value = "agentstatus")
    public Collection<AgentStatus> getAgentStatuses(@RequestParam Map<String, String> attributes) {
        return statusManager.getAgentStatuses();
    }

    @Secured(UserRoleConfiguration.WRITE_ACCESS_ROLE)
    @Operation(summary = "Clear the List of Agent Statuses", description = "Clears the list of connected agents")
    @DeleteMapping(value = "agentstatus")
    public void clearAgentStatuses(@RequestParam Map<String, String> attributes) {
        statusManager.reset();
    }
}
