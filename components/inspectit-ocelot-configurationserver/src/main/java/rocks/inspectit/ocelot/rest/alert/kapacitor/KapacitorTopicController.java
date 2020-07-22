package rocks.inspectit.ocelot.rest.alert.kapacitor;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.rest.alert.kapacitor.model.Handler;
import rocks.inspectit.ocelot.rest.alert.kapacitor.model.Topic;
import rocks.inspectit.ocelot.security.config.UserRoleConfiguration;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@RestController
public class KapacitorTopicController extends KapacitorBaseController {

    @Autowired
    public KapacitorTopicController(InspectitServerSettings settings) {
        super(settings);
    }

    @ApiOperation(value = "Provides a list of all available topics")
    @GetMapping("/alert/kapacitor/topics")
    public List<Topic> getTopics() {
        ObjectNode response = kapacitor()
                .getForEntity("/kapacitor/v1/alerts/topics", ObjectNode.class)
                .getBody();

        return StreamSupport.stream(response.path("topics").spliterator(), false)
                .map(Topic::fromKapacitorResponse)
                .collect(Collectors.toList());
    }

    @ApiOperation(value = "Provides a list of all available topics")
    @GetMapping("/alert/kapacitor/topics/{topicId}/handlers")
    public List<Handler> getHandlers(@PathVariable @ApiParam("The id of the topic whose handlers will be queried") String topicId) {
        ObjectNode response = kapacitor()
                .getForEntity("/kapacitor/v1/alerts/topics/{topicId}/handlers", ObjectNode.class, topicId)
                .getBody();

        return StreamSupport.stream(response.path("handlers").spliterator(), false)
                .map(Handler::fromKapacitorResponse)
                .collect(Collectors.toList());
    }

    @Secured(UserRoleConfiguration.WRITE_ACCESS_ROLE)
    @ApiOperation(value = "Adds a new Handler to a topic")
    @PostMapping("/alert/kapacitor/topics/{topicId}/handlers")
    public Handler addHandler(@PathVariable @ApiParam("The id of the topic to which the handler will be added") String topicId,
                              @RequestBody Handler handler) {
        ObjectNode response = kapacitor()
                .postForEntity("/kapacitor/v1/alerts/topics/{topicId}/handlers", handler.toKapacitorRequest(), ObjectNode.class, topicId)
                .getBody();

        return Handler.fromKapacitorResponse(response);
    }

    @Secured(UserRoleConfiguration.WRITE_ACCESS_ROLE)
    @ApiOperation(value = "Updates a handler of a given topic")
    @PatchMapping("/alert/kapacitor/topics/{topicId}/handlers/{handlerId}")
    public Handler updateHandler(@PathVariable @ApiParam("The id of the topic which owns the handler") String topicId,
                                 @PathVariable @ApiParam("The id of the handler") String handlerId,
                                 @RequestBody Handler handler) {
        ObjectNode response = kapacitor()
                .patchForObject("/kapacitor/v1/alerts/topics/{topicId}/handlers/{handlerId}",
                        handler.toKapacitorRequest(), ObjectNode.class, topicId, handlerId);

        return Handler.fromKapacitorResponse(response);
    }

    @Secured(UserRoleConfiguration.WRITE_ACCESS_ROLE)
    @ApiOperation(value = "Removes a handler from a topic")
    @DeleteMapping("/alert/kapacitor/topics/{topicId}/handlers/{handlerId}")
    public void removeHandler(@PathVariable @ApiParam("The id of the topic which owns the handler") String topicId,
                              @PathVariable @ApiParam("The id of the handler") String handlerId) {
        kapacitorRestTemplate.delete("/kapacitor/v1/alerts/topics/{topicId}/handlers/{handlerId}", topicId, handlerId);
    }

}
