package rocks.inspectit.ocelot.file.accessor.workingdirectory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.ocelot.file.FileInfo;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Proxy for {@link AutoCommitWorkingDirectoryProxy}. Caches the result of listFiles(String path) and retains it
 * for one day. Caches up to 10 different paths. Invalidates the cache as soon as a writing operation is executed.
 * These operations are: writeFile, createDirectory, move, delete.
 */
@Slf4j
public class CachingDirectoryProxy extends AbstractWorkingDirectoryAccessor{

    /**
     * Timeout used for the LoadingCache.
     */
    private static final Duration TIMEOUT_DURATION = Duration.ofDays(1);
    /**
     * Size used for the LoadingCache.
     */
    private static final long CACHE_SIZE = 10;

    /**
     * LoadingCache for the workspace. The keys represent a path, the values contains a List of {@link FileInfo}
     * instances, each of which resembles a file present in the workspace under the given path,
     */
    @VisibleForTesting
    LoadingCache<String, List<FileInfo>> workspaceCache;

    private final AutoCommitWorkingDirectoryProxy autoCommitWorkingDirectoryProxy;

    public CachingDirectoryProxy(AutoCommitWorkingDirectoryProxy autoCommitWorkingDirectoryProxy) {
        this.autoCommitWorkingDirectoryProxy = autoCommitWorkingDirectoryProxy;

        workspaceCache = CacheBuilder.newBuilder()
                .maximumSize(CACHE_SIZE)
                .expireAfterWrite(TIMEOUT_DURATION.toMillis(), TimeUnit.MILLISECONDS)
                .build(new CacheLoader<String, List<FileInfo>>() {
                    @Override
                    public List<FileInfo> load(String path) {
                        return autoCommitWorkingDirectoryProxy.listFiles(path);
                    }
                });
    }

    @Override
    protected String verifyPath(String relativeBasePath, String path) throws IllegalArgumentException {
        return autoCommitWorkingDirectoryProxy.verifyPath(relativeBasePath, path);
    }

    @Override
    protected byte[] readFile(String path) throws IOException {
        return autoCommitWorkingDirectoryProxy.readFile(path);
    }

    @Override
    protected List<FileInfo> listFiles(String path) {
        try {
            return workspaceCache.get(path);
        } catch (ExecutionException e) {
            log.error("An Exception occurred while reading path {} from working directory: {}", path, e.getMessage());
        }
        return Collections.emptyList();
    }

    @Override
    protected boolean exists(String path) {
        return autoCommitWorkingDirectoryProxy.exists(path);
    }

    @Override
    protected boolean isDirectory(String path) {
        return autoCommitWorkingDirectoryProxy.isDirectory(path);
    }

    @Override
    protected void writeFile(String path, String content) throws IOException {
        autoCommitWorkingDirectoryProxy.writeFile(path, content);
        workspaceCache.invalidateAll();
    }

    @Override
    protected void createDirectory(String path) throws IOException {
        autoCommitWorkingDirectoryProxy.createDirectory(path);
        workspaceCache.invalidateAll();
    }

    @Override
    protected void move(String sourcePath, String targetPath) throws IOException {
        autoCommitWorkingDirectoryProxy.move(sourcePath, targetPath);
        workspaceCache.invalidateAll();

    }

    @Override
    protected void delete(String path) throws IOException {
        autoCommitWorkingDirectoryProxy.delete(path);
        workspaceCache.invalidateAll();
    }
}
