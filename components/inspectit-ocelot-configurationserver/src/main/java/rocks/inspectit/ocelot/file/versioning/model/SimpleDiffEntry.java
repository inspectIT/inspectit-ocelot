package rocks.inspectit.ocelot.file.versioning.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.eclipse.jgit.diff.DiffEntry;
import rocks.inspectit.ocelot.file.accessor.AbstractFileAccessor;

import java.util.Collections;
import java.util.List;

/**
 * This class represents a single file diff including all information about it.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimpleDiffEntry {

    /**
     * Creates a {@link SimpleDiffEntry} based an a given {@link DiffEntry}.
     *
     * @param entry the {@link DiffEntry} to use as basis
     *
     * @return the created {@link SimpleDiffEntry}
     */
    public static SimpleDiffEntry of(DiffEntry entry) {
        SimpleDiffEntry simpleEntry = new SimpleDiffEntry();
        simpleEntry.setType(entry.getChangeType());

        if (entry.getChangeType() == DiffEntry.ChangeType.DELETE) {
            simpleEntry.setFile(entry.getOldPath());
        } else {
            simpleEntry.setFile(entry.getNewPath());
        }

        return simpleEntry;
    }

    /**
     * The filename, including its path. Example: /directory/file.yml
     */
    @NonNull
    private String file;

    /**
     * The type of modification.
     */
    @NonNull
    private DiffEntry.ChangeType type;

    /**
     * The file's current content. Only used in case a file is removed or modified.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String oldContent;

    /**
     * The file's new content. Only used in case a file is added or modified.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String newContent;

    /**
     * The list of authors who have modified this file since it was last promoted.
     */
    @Builder.Default
    private List<String> authors = Collections.emptyList();

    /**
     * Shortens the filename of this instance based on {@link AbstractFileAccessor#CONFIGURATION_FILES_SUBFOLDER}.
     * The shortened name is the original name without the subfolder prefix.
     * The name will not be shortened for the agent_mapping.yml
     *
     * @return the same {@link SimpleDiffEntry} object
     */
    public SimpleDiffEntry shortenName() {
        if(file.equals(AbstractFileAccessor.AGENT_MAPPINGS_FILE_NAME)) return this;

        file = file.substring(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER.length());
        return this;
    }
}
