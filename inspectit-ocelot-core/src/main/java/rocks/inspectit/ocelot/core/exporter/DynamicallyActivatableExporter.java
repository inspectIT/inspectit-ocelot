package rocks.inspectit.ocelot.core.exporter;

import rocks.inspectit.ocelot.config.model.InspectitConfig;

public interface DynamicallyActivatableExporter {

    boolean doDisable();

    boolean doEnable();

    boolean configurationChanged(InspectitConfig config);
    
}
