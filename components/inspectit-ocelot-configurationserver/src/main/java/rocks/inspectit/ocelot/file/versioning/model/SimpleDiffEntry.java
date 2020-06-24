package rocks.inspectit.ocelot.file.versioning.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.jgit.diff.DiffEntry;

@Data
@AllArgsConstructor
@NoArgsConstructor
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

    private String file;

    private DiffEntry.ChangeType type;

}
