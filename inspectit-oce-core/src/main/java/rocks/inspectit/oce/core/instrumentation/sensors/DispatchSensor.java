package rocks.inspectit.oce.core.instrumentation.sensors;

import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import rocks.inspectit.oce.core.instrumentation.config.InstrumentationProfileResolver;
import rocks.inspectit.oce.core.instrumentation.config.model.InstrumentationConfiguration;
import rocks.inspectit.oce.core.instrumentation.context.ContextManagerImpl;
import rocks.inspectit.oce.core.instrumentation.special.SpecialSensor;

@Component
@DependsOn(ContextManagerImpl.BEAN_NAME)
@Slf4j
public class DispatchSensor implements SpecialSensor {

    @Autowired
    private InstrumentationProfileResolver profileResolver;

    @Override
    public boolean shouldInstrument(TypeDescription type, InstrumentationConfiguration settings) {

        for (InstrumentationProfileResolver.ResolvedProfile profile : profileResolver.getActiveProfiles()) {
            boolean matches = profile.getTypeMatcher().matches(type);

            if (matches) {
                log.debug("Instrumenting {} due to profile '{}'.", type.getName(), profile.getName());
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean requiresInstrumentationChange(TypeDescription type, InstrumentationConfiguration first, InstrumentationConfiguration second) {
        return false;
    }

    @Override
    public DynamicType.Builder instrument(Class<?> clazz, TypeDescription type, InstrumentationConfiguration settings, DynamicType.Builder builder) {
        for (InstrumentationProfileResolver.ResolvedProfile profile : profileResolver.getActiveProfiles()) {
            boolean matches = profile.getTypeMatcher().matches(type);

            if (matches) {
                return builder.visit(Advice.to(DispatchSensorAdvice.class).on(profile.getMethodMatcher()));
            }
        }

        return builder;
    }
}
