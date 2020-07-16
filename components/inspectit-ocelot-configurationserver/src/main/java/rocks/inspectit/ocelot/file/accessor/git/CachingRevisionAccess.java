package rocks.inspectit.ocelot.file.accessor.git;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import rocks.inspectit.ocelot.file.FileInfo;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Decorates {@link RevisionAccess} with a cache to make subsequent lookups of the same files faster.
 */
public class CachingRevisionAccess extends RevisionAccess {

    /**
     * Maps file paths to their cached contents
     */
    private ConcurrentHashMap<String, byte[]> fileContentsCache = new ConcurrentHashMap<>();

    /**
     * Maps directory paths to their cached contents
     */
    private ConcurrentHashMap<String, List<FileInfo>> directoriesCache = new ConcurrentHashMap<>();

    /**
     * Constructor.
     *
     * @param repository the repository to use
     * @param revCommit  the commit which will be used for the operations
     */
    public CachingRevisionAccess(Repository repository, RevCommit revCommit) {
        super(repository, revCommit);
    }

    @Override
    protected byte[] readFile(String path) throws IOException {
        if (path != null) {
            byte[] cached = fileContentsCache.get(path);
            if (cached != null) {
                return cached;
            }
            byte[] contents = super.readFile(path);
            fileContentsCache.put(path, contents);
            return contents;
        } else {
            return super.readFile(null);
        }
    }

    @Override
    protected List<FileInfo> listFiles(String path) {
        if (path != null) {
            List<FileInfo> cached = directoriesCache.get(path);
            if (cached != null) {
                return cached;
            }
            List<FileInfo> fileInfos = super.listFiles(path);
            directoriesCache.put(path, fileInfos);
            return fileInfos;
        } else {
            return super.listFiles(null);
        }
    }
}
