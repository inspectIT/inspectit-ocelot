package rocks.inspectit.ocelot.rest.alert.kapacitor;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.rest.alert.kapacitor.model.Handler;
import rocks.inspectit.ocelot.rest.alert.kapacitor.model.Topic;
import rocks.inspectit.ocelot.security.config.UserRoleConfiguration;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@RestController
public class KapacitorTopicController extends KapacitorBaseController {

    @Autowired
    public KapacitorTopicController(InspectitServerSettings settings) {
        super(settings);
    }

    @Operation(summary = "Provides a list of all available topics")
    @GetMapping({"/alert/kapacitor/topics", "/alert/kapacitor/topics/"})
    public List<Topic> getTopics() {
        ObjectNode response = kapacitor()
                .getForEntity("/kapacitor/v1/alerts/topics", ObjectNode.class)
                .getBody();

        if (response == null) {
            return Collections.emptyList();
        }

        return StreamSupport.stream(response.path("topics").spliterator(), false)
                .map(Topic::fromKapacitorResponse)
                .collect(Collectors.toList());
    }

    @Operation(summary = "Provides a list of all available topics")
    @GetMapping({"/alert/kapacitor/topics/{topicId}/handlers", "/alert/kapacitor/topics/{topicId}/handlers/"})
    public List<Handler> getHandlers(@PathVariable @Parameter(description = "The id of the topic whose handlers will be queried") String topicId) {
        ObjectNode response = kapacitor()
                .getForEntity("/kapacitor/v1/alerts/topics/{topicId}/handlers", ObjectNode.class, topicId)
                .getBody();

        if (response == null) {
            return Collections.emptyList();
        }

        return StreamSupport.stream(response.path("handlers").spliterator(), false)
                .map(Handler::fromKapacitorResponse)
                .collect(Collectors.toList());
    }

    @Secured(UserRoleConfiguration.WRITE_ACCESS_ROLE)
    @Operation(summary = "Adds a new Handler to a topic")
    @PostMapping({"/alert/kapacitor/topics/{topicId}/handlers", "/alert/kapacitor/topics/{topicId}/handlers/"})
    public Handler addHandler(@PathVariable @Parameter(description = "The id of the topic to which the handler will be added") String topicId,
                              @RequestBody Handler handler) {
        ObjectNode response = kapacitor()
                .postForEntity("/kapacitor/v1/alerts/topics/{topicId}/handlers", handler.toKapacitorRequest(), ObjectNode.class, topicId)
                .getBody();

        return Handler.fromKapacitorResponse(response);
    }

    @Secured(UserRoleConfiguration.WRITE_ACCESS_ROLE)
    @Operation(summary = "Replaced a handler of a given topic")
    @PutMapping({"/alert/kapacitor/topics/{topicId}/handlers/{handlerId}", "/alert/kapacitor/topics/{topicId}/handlers/{handlerId}/"})
    public void replaceHandler(@PathVariable @Parameter(description = "The id of the topic which owns the handler") String topicId,
                               @PathVariable @Parameter(description = "The id of the handler to update") String handlerId,
                               @RequestBody Handler handler) {
        if (handler.getId() == null) {
            handler.setId(handlerId);
        }
        kapacitor().put("/kapacitor/v1/alerts/topics/{topicId}/handlers/{handlerId}",
                handler.toKapacitorRequest(), topicId, handlerId);
    }

    @Secured(UserRoleConfiguration.WRITE_ACCESS_ROLE)
    @Operation(summary = "Removes a handler from a topic")
    @DeleteMapping({"/alert/kapacitor/topics/{topicId}/handlers/{handlerId}", "/alert/kapacitor/topics/{topicId}/handlers/{handlerId}/"})
    public void removeHandler(@PathVariable @Parameter(description = "The id of the topic which owns the handler") String topicId,
                              @PathVariable @Parameter(description = "The id of the handler") String handlerId) {
        kapacitorRestTemplate.delete("/kapacitor/v1/alerts/topics/{topicId}/handlers/{handlerId}", topicId, handlerId);
    }

}
