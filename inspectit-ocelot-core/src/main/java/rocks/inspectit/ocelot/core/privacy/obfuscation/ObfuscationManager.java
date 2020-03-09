package rocks.inspectit.ocelot.core.privacy.obfuscation;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.privacy.obfuscation.ObfuscationPattern;
import rocks.inspectit.ocelot.config.model.privacy.obfuscation.ObfuscationSettings;
import rocks.inspectit.ocelot.core.config.InspectitConfigChangedEvent;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.privacy.obfuscation.impl.NoopObfuscatory;
import rocks.inspectit.ocelot.core.privacy.obfuscation.impl.PatternObfuscatory;
import rocks.inspectit.ocelot.core.privacy.obfuscation.impl.SelfMonitoringDelegatingObfuscatory;
import rocks.inspectit.ocelot.core.selfmonitoring.SelfMonitoringService;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Slf4j
public class ObfuscationManager {

    @Autowired
    private InspectitEnvironment env;

    @Autowired
    private SelfMonitoringService selfMonitoring;

    private IObfuscatory obfuscatory = NoopObfuscatory.INSTANCE;

    public Supplier<IObfuscatory> obfuscatorySupplier() {
        return () -> this.obfuscatory;
    }

    /**
     * Creates the new obfuscatory based on the event.
     */
    @EventListener(InspectitConfigChangedEvent.class)
    @PostConstruct
    @VisibleForTesting
    void update() {
        InspectitConfig configuration = env.getCurrentConfig();
        ObfuscationSettings obfuscationSettings = configuration.getPrivacy().getObfuscation();
        boolean enabled = obfuscationSettings.isEnabled();
        if (enabled) {
            this.obfuscatory = this.getPatternObfuscatory(obfuscationSettings.getPatterns());
        } else {
            this.obfuscatory = NoopObfuscatory.INSTANCE;
        }
    }

    /**
     * Creates {@link IObfuscatory} based on the collection of the {@link ObfuscationPattern}s.
     * <p>
     * If no pattern in the collection can successfully be compiled, then this method returns {@link NoopObfuscatory}.
     * Otherwise, the {@link PatternObfuscatory} instance will be created and return.
     *
     * @param obfuscationPatterns Collection of configured {@link ObfuscationPattern}s.
     * @return IObfuscatory
     */
    private IObfuscatory getPatternObfuscatory(Collection<ObfuscationPattern> obfuscationPatterns) {
        List<PatternObfuscatory.PatternEntry> patternEntries = Optional.ofNullable(obfuscationPatterns)
                .map(Collection::stream)
                .orElse(Stream.empty())
                .flatMap(p -> {
                    try {
                        int compileFlag = p.isCaseInsensitive() ? Pattern.CASE_INSENSITIVE : 0;
                        Pattern compiledPattern = Pattern.compile(p.getPattern(), compileFlag);
                        PatternObfuscatory.PatternEntry patternEntry = PatternObfuscatory.PatternEntry.builder()
                                .pattern(compiledPattern)
                                .checkKey(p.isCheckKey())
                                .checkData(p.isCheckData())
                                .replaceRegex(p.getReplaceRegex())
                                .build();
                        return Stream.of(patternEntry);
                    } catch (Exception e) {
                        log.warn("Failed to compile pattern {} for the data obfuscation. Skipping..", p.getPattern(), e);
                        return Stream.empty();
                    }
                })
                .collect(Collectors.toList());

        if (patternEntries.isEmpty()) {
            return NoopObfuscatory.INSTANCE;
        } else {
            PatternObfuscatory patternObfuscatory = new PatternObfuscatory(patternEntries);
            if (selfMonitoring.isSelfMonitoringEnabled()) {
                return new SelfMonitoringDelegatingObfuscatory(selfMonitoring, patternObfuscatory);
            } else {
                return patternObfuscatory;
            }
        }
    }

}
