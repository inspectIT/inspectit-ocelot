package rocks.inspectit.ocelot.rest.agent;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;
import rocks.inspectit.ocelot.config.model.InspectitConfig;

import java.util.Collections;
import java.util.Map;
import java.util.Set;


@RestController
@RequestMapping("/agent")
public class AgentController {

    @GetMapping("/configuration")
    @ApiImplicitParam(name = "Authorization", value = "Bearer token for authorization", required = true, dataType = "string", paramType = "header")
    @ApiOperation(value = "Fetch configuration", notes = "This is used by agents to fetch their current configuration.")
    public String fetchConfiguration(@ApiParam(value = "The agent's service name", required = true) @RequestParam String serviceName) {
        InspectitConfig config = new InspectitConfig();
        Map<String, InspectitConfig> configMap = Collections.singletonMap("inspectit", config);

        config.setServiceName(serviceName + System.currentTimeMillis());

        Yaml yaml = new Yaml(new Representer() {
            @Override
            protected MappingNode representJavaBean(Set<Property> properties, Object javaBean) {
                if (!classTags.containsKey(javaBean.getClass()))
                    addClassTag(javaBean.getClass(), Tag.MAP);

                return super.representJavaBean(properties, javaBean);
            }
        });
        return yaml.dumpAs(configMap, Tag.MAP, null);
    }
}
