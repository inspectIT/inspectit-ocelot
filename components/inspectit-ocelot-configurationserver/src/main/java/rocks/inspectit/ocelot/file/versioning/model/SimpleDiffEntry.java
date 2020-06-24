package rocks.inspectit.ocelot.file.versioning.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.eclipse.jgit.diff.DiffEntry;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimpleDiffEntry {

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

    @NonNull
    private String file;

    @NonNull
    private DiffEntry.ChangeType type;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String oldContent;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String newContent;
}
