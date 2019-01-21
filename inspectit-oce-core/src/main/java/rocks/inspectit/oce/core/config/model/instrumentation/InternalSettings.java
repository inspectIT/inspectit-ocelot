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
     * The time to pause between executing batches of class instrumentation updates
     */
    private Duration interBatchDelay;

    /**
     * Defines the maximum number classes which are checked at once for updates of their configuration per batch
     */
    @Min(50)
    private int classConfigurationCheckBatchSize;

    /**
     * Defines the maximum number classes which are retransformed at once per batch
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

    /**
     * Defines how long after the invocation of ClassFileTransform the agent should wait with trying to discover new classes.
     * The default value is set to zero for cases where the application constantly keeps defining new classes.
     */
    private Duration minClassDefinitionDelay;
}
