package rocks.inspectit.oce.eum.server.configuration.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import rocks.inspectit.oce.eum.server.utils.DefaultTags;
import rocks.inspectit.ocelot.config.model.tags.TagsSettings;

import javax.validation.constraints.AssertFalse;
import javax.validation.constraints.AssertTrue;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * Holds an additional map of tags, which will be resolved based on the EUM beacon.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class EumTagsSettings extends TagsSettings {

    /**
     * Maps tag name to beacon key.
     */
    private final Map<String, String> beacon = new HashMap<>();

    /**
     * List of tags, which are defined as global
     */
    private final Set<String> defineAsGlobal = new HashSet<>();

    @AssertFalse(message = "All defined global tags should exist either in extra tags or beacon tags")
    public boolean isGlobalTagMissing() {
        return defineAsGlobal.stream()
                .anyMatch(globalTag ->
                        !(getExtra().containsKey(globalTag)
                                || getBeacon().containsKey(globalTag)
                                || DefaultTags.isDefaultTag(globalTag)));
    }

    @AssertTrue(message = "Each tag should only be defined once")
    public boolean isCheckUniquenessOfTags() {
        return getExtra().keySet().stream().allMatch(extraTag -> !beacon.containsKey(extraTag));
    }
}
