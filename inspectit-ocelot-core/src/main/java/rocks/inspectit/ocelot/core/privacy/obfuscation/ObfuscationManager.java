package rocks.inspectit.ocelot.core.privacy.obfuscation;

import com.google.common.annotations.VisibleForTesting;
import io.opencensus.tags.TagContextBuilder;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.TagValue;
import io.opencensus.tags.Tags;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.privacy.obfuscation.ObfuscationPattern;
import rocks.inspectit.ocelot.config.model.privacy.obfuscation.ObfuscationSettings;
import rocks.inspectit.ocelot.core.config.InspectitConfigChangedEvent;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.privacy.obfuscation.impl.NoopObfuscatory;
import rocks.inspectit.ocelot.core.privacy.obfuscation.impl.PatternObfuscatory;

import javax.annotation.PostConstruct;
import javax.validation.Valid;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Slf4j
public class ObfuscationManager {

    @Autowired
    private InspectitEnvironment env;

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

        IObfuscatory obfuscatory = NoopObfuscatory.INSTANCE;
        @Valid ObfuscationSettings obfuscationSettings = configuration.getPrivacy().getObfuscation();
        boolean enabled = obfuscationSettings.isEnabled();
        if (enabled) {
            obfuscatory = this.getPatternObfuscatory(obfuscationSettings.getPatterns(), obfuscationSettings.isCaseInsensitive());
        }

        // TODO is this OK concurrency wise?
        this.obfuscatory = obfuscatory;
    }

    /**
     * Creates {@link IObfuscatory} based on the collection of the {@link ObfuscationPattern}s.
     * <p>
     * If no pattern in the collection can successfully be compiled, then this method returns {@link NoopObfuscatory}.
     * Otherwise, the {@link PatternObfuscatory} instance will be created and return.
     *
     * @param obfuscationPatterns Collection of configured {@link ObfuscationPattern}s.
     * @param caseInsensitive     If pattern compilation is case insensitive.
     * @return IObfuscatory
     */
    private IObfuscatory getPatternObfuscatory(Collection<ObfuscationPattern> obfuscationPatterns, boolean caseInsensitive) {
        List<PatternObfuscatory.PatternEntry> patternEntries = Optional.ofNullable(obfuscationPatterns)
                .map(Collection::stream)
                .orElse(Stream.empty())
                .flatMap(p -> {
                    try {
                        Pattern compiledPattern = caseInsensitive ? Pattern.compile(p.getPattern(), Pattern.CASE_INSENSITIVE) : Pattern.compile(p.getPattern());
                        PatternObfuscatory.PatternEntry patternEntry = PatternObfuscatory.PatternEntry.builder()
                                .pattern(compiledPattern)
                                .checkKey(p.isCheckKey())
                                .checkData(p.isCheckData())
                                .build();
                        return Stream.of(patternEntry);
                    } catch (Exception e) {
                        log.warn("Failed to compile pattern {} for the data obfuscation. Skipping..", p.getPattern());
                        log.debug("Error compiling pattern for the data obfuscation.", e);
                        return Stream.empty();
                    }
                })
                .collect(Collectors.toList());

        if (patternEntries.isEmpty()) {
            return NoopObfuscatory.INSTANCE;
        } else {
            return new PatternObfuscatory(patternEntries);
        }
    }

}
