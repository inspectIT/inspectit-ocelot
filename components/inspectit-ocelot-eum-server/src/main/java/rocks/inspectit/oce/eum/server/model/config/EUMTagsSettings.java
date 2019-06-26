package rocks.inspectit.oce.eum.server.model.config;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import rocks.inspectit.oce.eum.server.utils.DefaultTags;
import rocks.inspectit.ocelot.config.model.tags.TagsSettings;

import javax.validation.constraints.AssertTrue;
import java.util.*;


/**
 * Holds an additional map of tags, which will be resolved based on the EUM beacon.
 */
@Data
@EqualsAndHashCode(callSuper=true)
@NoArgsConstructor
public class EUMTagsSettings extends TagsSettings {
    /**
     * Maps tag name to beacon key.
     */
    private final Map<String, String> beacon = new HashMap<>();


    /**
     * List of tags, which are defined as global
     */
    private final Set<String> global = new HashSet<>();

    @AssertTrue(message = "All defined global tags should exist either in extra tags or beacon tags")
    public boolean isGlobalTagsContainAlreadyDefinedTags() {
        return global.stream().allMatch(globalTag -> getExtra().containsKey(globalTag) || getBeacon().containsKey(globalTag) || DefaultTags.isDefaultTag(globalTag));
    }

    @AssertTrue(message = "Each tag should only be defined once")
    public boolean isCheckUniquenessOfTags() {
        return getExtra().keySet().stream().allMatch(extraTag -> !beacon.containsKey(extraTag));
    }
}
