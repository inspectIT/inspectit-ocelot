package rocks.inspectit.oce.core.instrumentation.special;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import rocks.inspectit.oce.core.config.model.instrumentation.InstrumentationSettings;

public interface SpecialSensor {

    boolean shouldInstrument(TypeDescription type, InstrumentationSettings settings);

    boolean requiresInstrumentationChange(TypeDescription type, InstrumentationSettings first, InstrumentationSettings second);

    DynamicType.Builder instrument(TypeDescription type, InstrumentationSettings settings, DynamicType.Builder builder);

}
