package rocks.inspectit.oce.core.config.model.instrumentation;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import java.time.Duration;

@Data
@NoArgsConstructor
public class InternalSettings {

    private Duration classConfigurationCheckInterBatchDelay;

    @Min(50)
    private int classConfigurationCheckBatchSize;

    private Duration classRetransformInterBatchDelay;

    @Min(1)
    private int classRetransformBatchSize;


    private Duration newClassDiscoveryInterval;

    private Duration maxClassDefinitionDelay;
}
