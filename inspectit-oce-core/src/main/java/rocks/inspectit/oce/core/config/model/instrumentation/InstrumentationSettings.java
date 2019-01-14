package rocks.inspectit.oce.core.config.model.instrumentation;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;

@Data
@NoArgsConstructor
public class InstrumentationSettings {

    @Valid
    private InternalSettings internal;

    @Valid
    private SpecialSettings special;

}
