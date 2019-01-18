package rocks.inspectit.oce.core.config.model.instrumentation;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import java.time.Duration;

/**
 * Configuration object allowing a fine-tuning of the instrumentation process.
 */
@Data
@NoArgsConstructor
public class InternalSettings {

    /**
     * The time to pause between executing batches of class instrumentation configuration checks
     */
    private Duration classConfigurationCheckInterBatchDelay;

    /**
     * Defines how many classes are checked at once for updates of their configuration
     */
    @Min(50)
    private int classConfigurationCheckBatchSize;

    /**
     * Defines the time to pause between calls to Instrumentation.retransform
     */
    private Duration classRetransformInterBatchDelay;

    /**
     * Defines how many classes are retransformed at once
     */
    @Min(1)
    private int classRetransformBatchSize;

    /**
     * Defines how often the Agent should check if new classes have been defined.
     * This check is only performed if Classloader.defineClass was called less than {@link #maxClassDefinitionDelay} ago.
     */
    private Duration newClassDiscoveryInterval;

    /**
     * Defines how long after the invocation of ClassFileTransform the class is guaranteed to be present in Instrumentation.getAllLoadedClasses.
     * This influences if newly created classes are discovered by inspectIT.
     */
    private Duration maxClassDefinitionDelay;
}
