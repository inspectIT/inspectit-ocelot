package rocks.inspectit.ocelot.rest.alert.kapacitor;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

    /**
     * URL path of Kapacitor's task endpoint.
     */
    private static final String KAPACITOR_PATH_TASKS = "/kapacitor/v1/tasks";

    /**
     * URL path of Kapacitor's task endpoint for referencing a specific task by its ID.
     */
    private static final String KAPACITOR_PATH_TASKS_ID = "/kapacitor/v1/tasks/{taskId}";

    /**
     * URL path used by the configuration server for providing task management.
     */
    private static final String ALERT_PATH_TASKS = "/alert/kapacitor/tasks";

    /**
     * URL path used by the configuration server for providing task management of a specific task.
     */
    private static final String ALERT_PATH_TASKS_ID = "/alert/kapacitor/tasks/{taskId}";

    @Autowired
    public KapacitorTaskController(InspectitServerSettings settings) {
        super(settings);
    }

    @Operation(summary = "Provides a list with basic information about each kapacitor task. Only tasks based on templates will be listed.")
    @GetMapping({ALERT_PATH_TASKS, ALERT_PATH_TASKS + "/"})
    public List<Task> getAllTasks() {
        ObjectNode response = kapacitor().getForEntity(KAPACITOR_PATH_TASKS, ObjectNode.class).getBody();

        if (response == null) {
            return Collections.emptyList();
        }
        return StreamSupport.stream(response.path("tasks").spliterator(), false)
                .map(Task::fromKapacitorResponse)
                .filter(task -> task.getTemplate() != null)
                .collect(Collectors.toList());
    }

    @Operation(summary = "Provides detailed information about a given kapacitor task")

    @GetMapping({ALERT_PATH_TASKS_ID, ALERT_PATH_TASKS_ID + "/"})
    public Task getTask(@PathVariable @Parameter(description = "The id of the task to query") String taskId) {
        ObjectNode response = kapacitor().getForEntity(KAPACITOR_PATH_TASKS_ID, ObjectNode.class, taskId).getBody();

        return Task.fromKapacitorResponse(response);
    }

    @Secured(UserRoleConfiguration.WRITE_ACCESS_ROLE)
    @Operation(summary = "Inserts a new Kapacitor task")
    @PostMapping({ALERT_PATH_TASKS, ALERT_PATH_TASKS + "/"})
    public Task addTask(@RequestBody Task task) {
        ObjectNode response = kapacitor().postForEntity(KAPACITOR_PATH_TASKS, task.toKapacitorRequest(), ObjectNode.class)
                .getBody();

        return Task.fromKapacitorResponse(response);
    }

    @Secured(UserRoleConfiguration.WRITE_ACCESS_ROLE)
    @Operation(summary = "Updates one or more settings of a kapacitor task")
    @PatchMapping({ALERT_PATH_TASKS_ID, ALERT_PATH_TASKS_ID + "/"})
    public Task updateTask(@PathVariable @Parameter(description = "The id of the task to update") String taskId, @RequestBody Task task) {
        ObjectNode response = kapacitor().patchForObject(KAPACITOR_PATH_TASKS_ID, task.toKapacitorRequest(), ObjectNode.class, taskId);
        triggerTaskReload(task);

        return Task.fromKapacitorResponse(response);
    }

    @Secured(UserRoleConfiguration.WRITE_ACCESS_ROLE)
    @Operation(summary = "Removes a task")
    @DeleteMapping({ALERT_PATH_TASKS_ID, ALERT_PATH_TASKS_ID + "/"})
    public void removeTask(@PathVariable @Parameter(description = "The id of the task to delete") String taskId) {
        kapacitorRestTemplate.delete(KAPACITOR_PATH_TASKS_ID, taskId);
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
