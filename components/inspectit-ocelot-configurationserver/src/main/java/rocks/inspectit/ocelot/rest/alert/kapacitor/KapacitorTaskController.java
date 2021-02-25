package rocks.inspectit.ocelot.rest.alert.kapacitor;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.rest.alert.kapacitor.model.Task;
import rocks.inspectit.ocelot.security.config.UserRoleConfiguration;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@RestController
public class KapacitorTaskController extends KapacitorBaseController {

    @Autowired
    public KapacitorTaskController(InspectitServerSettings settings) {
        super(settings);
    }

    @ApiOperation(value = "Provides a list with basic information about each kapacitor task. Only tasks based on templates will be listed.")
    @GetMapping("/alert/kapacitor/tasks")
    public List<Task> getAllTasks() {
        ObjectNode response = kapacitor().getForEntity("/kapacitor/v1/tasks", ObjectNode.class).getBody();

        return StreamSupport.stream(response.path("tasks").spliterator(), false)
                .map(Task::fromKapacitorResponse)
                .filter(task -> task.getTemplate() != null)
                .collect(Collectors.toList());
    }

    @ApiOperation(value = "Provides detailed information about a given kapacitor task")

    @GetMapping("/alert/kapacitor/tasks/{taskId}")
    public Task getTask(@PathVariable @ApiParam("The id of the task to query") String taskId) {
        ObjectNode response = kapacitor().getForEntity("/kapacitor/v1/tasks/{taskId}", ObjectNode.class, taskId)
                .getBody();

        return Task.fromKapacitorResponse(response);
    }

    @Secured(UserRoleConfiguration.WRITE_ACCESS_ROLE)
    @ApiOperation(value = "Inserts a new Kapacitor task")
    @PostMapping("/alert/kapacitor/tasks")
    public Task addTask(@RequestBody Task task) {
        ObjectNode response = kapacitor().postForEntity("/kapacitor/v1/tasks", task.toKapacitorRequest(), ObjectNode.class)
                .getBody();

        return Task.fromKapacitorResponse(response);
    }

    @Secured(UserRoleConfiguration.WRITE_ACCESS_ROLE)
    @ApiOperation(value = "Updates one or more settings of a kapacitor task")
    @PatchMapping("/alert/kapacitor/tasks/{taskId}")
    public Task updateTask(@PathVariable @ApiParam("The id of the task to update") String taskId, @RequestBody Task task) {
        ObjectNode response = kapacitor().patchForObject("/kapacitor/v1/tasks/{taskId}", task.toKapacitorRequest(), ObjectNode.class, taskId);
        triggerTaskReload(task);

        return Task.fromKapacitorResponse(response);
    }

    @Secured(UserRoleConfiguration.WRITE_ACCESS_ROLE)
    @ApiOperation(value = "Removes a task")
    @DeleteMapping("/alert/kapacitor/tasks/{taskId}")
    public void removeTask(@PathVariable @ApiParam("The id of the task to delete") String taskId) {
        kapacitorRestTemplate.delete("/kapacitor/v1/tasks/{taskId}", taskId);
    }

    /**
     * Required to reload kapacitor tasks during runtime.
     * A task cannot be reloaded and just doing patch request against the template API - without changing it -
     * triggers kapacitor to reload the previously changed task.
     * Note that all tasks created from this template will be reloaded.
     *
     * @param task kapacitor task
     */
    private void triggerTaskReload(Task task) {
        String taskTemplateId = task.getTemplate();
        kapacitor().patchForObject("/kapacitor/v1/templates/{templateID}", task.toKapacitorRequestTemplateUpdate(), ObjectNode.class, taskTemplateId);
    }

}
