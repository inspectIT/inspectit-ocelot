package rocks.inspectit.ocelot.file.versioning.remoteconfiguration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;

@Component
public class RemoteConfigurationManager {

    @Autowired
    private InspectitServerSettings settings;

}
