package rocks.inspectit.ocelot.rest.agentstatus;

import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rocks.inspectit.ocelot.agentstatus.AgentStatus;
import rocks.inspectit.ocelot.agentstatus.AgentStatusManager;
import rocks.inspectit.ocelot.rest.AbstractBaseController;

import java.util.Collection;
import java.util.Map;


/**
 * The rest controller providing the interface used by the agent for configuration fetching.
 */
@RestController
public class AgentStatusController extends AbstractBaseController {

    @Autowired
    private AgentStatusManager statusManager;

    @ApiOperation(value = "Fetch the List of Agent Statuses", notes = "Gives a list of connected agents")
    @GetMapping(value = "agentstatus")
    public Collection<AgentStatus> fetchAgentStatuses(@RequestParam Map<String, String> attributes) {
        return statusManager.getAgentStatuses();
    }

    @ApiOperation(value = "Clear the List of Agent Statuses", notes = "Clears the list of connected agents")
    @DeleteMapping(value = "agentstatus")
    public void clearAgentStatuses(@RequestParam Map<String, String> attributes) {
        statusManager.reset();
    }
}
