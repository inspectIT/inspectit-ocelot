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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@RestController
public class KapacitorTaskController extends KapacitorBaseController {

    private static final String TASK_PATH = "/kapacitor/v1/tasks";
    private static final String TASK_ID_PATH = "/kapacitor/v1/tasks/{taskId}";
    private static final String ALERT_TASK_PATH = "/alert/kapacitor/tasks";
    private static final String ALERT_TASK_ID_PATH = "/alert/kapacitor/tasks/{taskId}";

    @Autowired
    public KapacitorTaskController(InspectitServerSettings settings) {
        super(settings);
    }

    @ApiOperation(value = "Provides a list with basic information about each kapacitor task. Only tasks based on templates will be listed.")
    @GetMapping(ALERT_TASK_PATH)
    public List<Task> getAllTasks() {
        ObjectNode response = kapacitor().getForEntity(TASK_PATH, ObjectNode.class).getBody();

        if (response == null) {
            return Collections.emptyList();
        }
        return StreamSupport.stream(response.path("tasks").spliterator(), false)
                .map(Task::fromKapacitorResponse)
                .filter(task -> task.getTemplate() != null)
                .collect(Collectors.toList());
    }

    @ApiOperation(value = "Provides detailed information about a given kapacitor task")

    @GetMapping(ALERT_TASK_ID_PATH)
    public Task getTask(@PathVariable @ApiParam("The id of the task to query") String taskId) {
        ObjectNode response = kapacitor().getForEntity(TASK_ID_PATH, ObjectNode.class, taskId)
                .getBody();

        return Task.fromKapacitorResponse(response);
    }

    @Secured(UserRoleConfiguration.WRITE_ACCESS_ROLE)
    @ApiOperation(value = "Inserts a new Kapacitor task")
    @PostMapping(ALERT_TASK_PATH)
    public Task addTask(@RequestBody Task task) {
        ObjectNode response = kapacitor().postForEntity(TASK_PATH, task.toKapacitorRequest(), ObjectNode.class)
                .getBody();

        return Task.fromKapacitorResponse(response);
    }

    @Secured(UserRoleConfiguration.WRITE_ACCESS_ROLE)
    @ApiOperation(value = "Updates one or more settings of a kapacitor task")
    @PatchMapping(ALERT_TASK_ID_PATH)
    public Task updateTask(@PathVariable @ApiParam("The id of the task to update") String taskId, @RequestBody Task task) {
        ObjectNode response = kapacitor().patchForObject(TASK_ID_PATH, task.toKapacitorRequest(), ObjectNode.class, taskId);
        triggerTaskReload(task);

        return Task.fromKapacitorResponse(response);
    }

    @Secured(UserRoleConfiguration.WRITE_ACCESS_ROLE)
    @ApiOperation(value = "Removes a task")
    @DeleteMapping(ALERT_TASK_ID_PATH)
    public void removeTask(@PathVariable @ApiParam("The id of the task to delete") String taskId) {
        kapacitorRestTemplate.delete(TASK_ID_PATH, taskId);
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
