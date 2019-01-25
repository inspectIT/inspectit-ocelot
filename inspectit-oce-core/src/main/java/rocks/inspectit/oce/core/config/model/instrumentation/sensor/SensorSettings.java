package rocks.inspectit.oce.core.config.model.instrumentation.sensor;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class SensorSettings {

    private List<SensorTargetType> targets;

    private List<SensorTargetMethod> methods;

}
