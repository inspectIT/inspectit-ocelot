package rocks.inspectit.ocelot.config.model.instrumentation;

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
    // default value is there for testing to ensure a default constructed InternalSettings is valid.
    // The actual default value is defined in the default.yml
    private int classConfigurationCheckBatchSize = 50;

    /**
     * Defines the maximum number classes which are retransformed at once per batch
     */
    @Min(1)
    // default value is there for testing to ensure a default constructed InternalSettings is valid.
    // The actual default value is defined in the default.yml
    private int classRetransformBatchSize = 1; //default value for testing

    /**
     * Defines how often the Agent should check if new classes have been defined.
     * This check is only performed if Classloader.defineClass was called less than {@link #numClassDiscoveryTrials} ago.
     */
    private Duration newClassDiscoveryInterval;

    /**
     * Defines how often after the last invocation of a ClassFileTransformer the agent scans for new classes.
     */
    private int numClassDiscoveryTrials;

    /**
     * "Actions" are defined by injecting classes into existing classloaders.
     * When a security manager is enabled, it is important that these classes have a {@link java.security.ProtectionDomain}
     * configured. Depending on the security needs, this can be either the PD of inspectit or of a neighbor-class in the Classloader.
     */
    private boolean useInspectitProtectionDomain;

    /**
     * Defines whether orphan action classes are recycled or new classes should be injected instead.
     */
    private boolean recyclingOldActionClasses = true;

    /**
     * Flag ensures synchronous instrumentation of class. This means, each class is instrumented on first class load.
     * <p>
     * Important: In Java runtimes lower than 9, this results in a significant boot time performance degradation! See: <a href="https://bugs.openjdk.java.net/browse/JDK-7018422">JDK-7018422</a>"
     */
    private boolean async = true;

}
