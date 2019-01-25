package rocks.inspectit.oce.core.instrumentation.config;

import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.NameMatcher;
import net.bytebuddy.matcher.StringMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import rocks.inspectit.oce.core.config.InspectitConfigChangedEvent;
import rocks.inspectit.oce.core.config.InspectitEnvironment;
import rocks.inspectit.oce.core.config.model.instrumentation.InstrumentationProfile;
import rocks.inspectit.oce.core.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.oce.core.config.model.instrumentation.sensor.SensorSettings;
import rocks.inspectit.oce.core.config.model.instrumentation.sensor.SensorTargetMethod;
import rocks.inspectit.oce.core.config.model.instrumentation.sensor.SensorTargetType;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

import static net.bytebuddy.matcher.ElementMatchers.*;

@Service
@Slf4j
public class InstrumentationProfileResolver {

    @Autowired
    private InspectitEnvironment env;

    @Getter
    private List<ResolvedProfile> activeProfiles = Collections.emptyList();

    @PostConstruct
    private void init() {
        processConfiguration(env.getCurrentConfig().getInstrumentation());
    }

    @EventListener
    private void inspectitConfigChanged(InspectitConfigChangedEvent event) {
        processConfiguration(event.getNewConfig().getInstrumentation());
    }

    private void processConfiguration(InstrumentationSettings settings) {
        log.debug("Resolve profiles.");

        List<ResolvedProfile> resolvedProfiles = new ArrayList<>();

        List<InstrumentationProfile> profiles = settings.getInstrumentationProfiles();

        for (InstrumentationProfile profile : profiles) {
            ResolvedProfile resolvedProfile = new ResolvedProfile();
            resolvedProfile.name = profile.getName();
            resolvedProfile.priority = profile.getPriority();

            List<SensorSettings> profileSensors = profile.getSensors().entrySet().stream()
                    .filter(Map.Entry::getValue)
                    .map(Map.Entry::getKey)
                    .map(key -> settings
                            .getAvailableSensors().entrySet().stream()
                            .filter(entry -> entry.getKey().equals(key))
                            .map(Map.Entry::getValue)
                            .findFirst()
                    ).filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());


            List<SensorTargetType> targetTypes = profileSensors.stream()
                    .map(SensorSettings::getTargets)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());

            List<SensorTargetMethod> targetMethods = profileSensors.stream()
                    .map(SensorSettings::getMethods)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());

            resolvedProfile.typeMatcher = createTypeMatcher(targetTypes);
            resolvedProfile.methodMatcher = createMethodMatcher(targetMethods);

            resolvedProfiles.add(resolvedProfile);
        }

        activeProfiles = resolvedProfiles;
    }

    private ElementMatcher.Junction<TypeDescription> createTypeMatcher(List<SensorTargetType> targetTypes) {
        TargetMatcherBuilder<TypeDescription> builder = new TargetMatcherBuilder<>();

        for (SensorTargetType targetType : targetTypes) {
            builder.and(targetType.getIsAbstract(), isAbstract());
            builder.and(targetType.getIsInterface(), isInterface());
            builder.and(new NameMatcher<>(new StringMatcher(targetType.getNamePattern(), targetType.getMatcherMode())));
            builder.next();
        }

        return builder.build();
    }

    private ElementMatcher.Junction<MethodDescription> createMethodMatcher(List<SensorTargetMethod> targetMethods) {
        TargetMatcherBuilder<MethodDescription> builder = new TargetMatcherBuilder<>();

        for (SensorTargetMethod targetMethod : targetMethods) {
            TargetMatcherBuilder<MethodDescription> visibilityMatcherBuilder = new TargetMatcherBuilder<>();
            visibilityMatcherBuilder.and(targetMethod.getIsPublic(), isPublic());
            visibilityMatcherBuilder.next();
            visibilityMatcherBuilder.and(targetMethod.getIsProtected(), isProtected());
            visibilityMatcherBuilder.next();
            visibilityMatcherBuilder.and(targetMethod.getIsPackagePrivate(), isPackagePrivate());
            visibilityMatcherBuilder.next();
            visibilityMatcherBuilder.and(targetMethod.getIsPrivate(), isPrivate());
            ElementMatcher.Junction<MethodDescription> visibilityMatcher = visibilityMatcherBuilder.build();

            builder.and(targetMethod.getIsConstructor(), isConstructor());
            builder.and(targetMethod.getIsSynchronized(), isSynchronized());
            builder.and(visibilityMatcher);
            builder.and(new NameMatcher<>(new StringMatcher(targetMethod.getNamePattern(), targetMethod.getMatcherMode())));

            if (targetMethod.getArguments() != null) {
//            for (SensorTargetMethod.Argument argument : targetMethod.getArguments()) {
                for (Map.Entry<Integer, String> argument : targetMethod.getArguments().entrySet()) {
                    int index = argument.getKey();
                    String className = argument.getValue();
                    builder.and(takesArgument(index, named(className)));
                }
            }

            builder.next();
        }

        return builder.build();
    }

    @Data
    public class ResolvedProfile {

        private String name;

        private int priority;

        private ElementMatcher.Junction<TypeDescription> typeMatcher;

        private ElementMatcher.Junction<MethodDescription> methodMatcher;
    }
}
