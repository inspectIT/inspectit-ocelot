package rocks.inspectit.ocelot.config.model.metrics;

import lombok.Data;
import lombok.NoArgsConstructor;
import rocks.inspectit.ocelot.config.validation.AdditionalValidations;

import java.time.Duration;

@Data
@NoArgsConstructor
@AdditionalValidations
public class TagGuardSettings {

    /**
     *
     */
    private Duration scheduleDelay;

    /**
     *
     */
    private String databaseFile;


    private String overflowReplacement;

}
