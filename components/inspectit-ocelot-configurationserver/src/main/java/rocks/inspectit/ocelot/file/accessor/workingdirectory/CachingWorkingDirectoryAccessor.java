package rocks.inspectit.ocelot.file.accessor.workingdirectory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.ocelot.file.FileInfo;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Proxy for {@link AbstractWorkingDirectoryAccessor}. Caches the result of listFiles(String path) and retains it
 * for one day. Caches up to 10 different paths. Invalidates the cache as soon as a writing operation is executed.
 * These operations are: writeFile, createDirectory, move, delete.
 */
@Slf4j
public class CachingWorkingDirectoryAccessor extends AbstractWorkingDirectoryAccessor {

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

    @Delegate(excludes = ExcludedListMethods.class)
    private final AbstractWorkingDirectoryAccessor workingDirectoryAccessor;

    public CachingWorkingDirectoryAccessor(AbstractWorkingDirectoryAccessor autoCommitWorkingDirectoryProxy) {
        this.workingDirectoryAccessor = autoCommitWorkingDirectoryProxy;

        workspaceCache = CacheBuilder.newBuilder()
                .maximumSize(CACHE_SIZE)
                .expireAfterWrite(TIMEOUT_DURATION.toMillis(), TimeUnit.MILLISECONDS)
                .build(new CacheLoader<String, List<FileInfo>>() {
                    @Override
                    public List<FileInfo> load(String path) {
                        return workingDirectoryAccessor.listConfigurationFiles(path);
                    }
                });
    }

    /**
     * Invalidates the current cache.
     */
    public void invalidateCache() {
        workspaceCache.invalidateAll();
    }

    @Override
    public List<FileInfo> listConfigurationFiles(String path) {
        try {
            return workspaceCache.get(path);
        } catch (ExecutionException e) {
            log.error("An Exception occurred while reading path {} from working directory: {}", path, e.getMessage());
        }
        return Collections.emptyList();
    }

    @Override
    public void writeAgentMappings(String content) throws IOException {
        workingDirectoryAccessor.writeAgentMappings(content);
        workspaceCache.invalidateAll();
    }

    @Override
    public void writeConfigurationFile(String file, String content) throws IOException {
        workingDirectoryAccessor.writeConfigurationFile(file, content);
        workspaceCache.invalidateAll();
    }

    @Override
    public void createConfigurationDirectory(String directory) throws IOException {
        workingDirectoryAccessor.createConfigurationDirectory(directory);
        workspaceCache.invalidateAll();
    }

    @Override
    public void moveConfiguration(String source, String target) throws IOException {
        workingDirectoryAccessor.moveConfiguration(source, target);
        workspaceCache.invalidateAll();

    }

    @Override
    public void deleteConfiguration(String path) throws IOException {
        workingDirectoryAccessor.deleteConfiguration(path);
        workspaceCache.invalidateAll();
    }

    /**
     * Not used because the call is delegated via {@link Delegate} to the {@link #workingDirectoryAccessor}.
     */
    @Override
    protected String verifyPath(String relativeBasePath, String path) throws IllegalArgumentException {
        return null;
    }

    /**
     * Not used because the call is delegated via {@link Delegate} to the {@link #workingDirectoryAccessor}.
     */
    @Override
    protected void writeFile(String path, String content) {
    }

    /**
     * Not used because the call is delegated via {@link Delegate} to the {@link #workingDirectoryAccessor}.
     */
    @Override
    protected void createDirectory(String path) {
    }

    /**
     * Not used because the call is delegated via {@link Delegate} to the {@link #workingDirectoryAccessor}.
     */
    @Override
    protected void move(String sourcePath, String targetPath) throws IOException {
    }

    /**
     * Not used because the call is delegated via {@link Delegate} to the {@link #workingDirectoryAccessor}.
     */
    @Override
    protected void delete(String path) throws IOException {
    }

    /**
     * Not used because the call is delegated via {@link Delegate} to the {@link #workingDirectoryAccessor}.
     */
    @Override
    protected byte[] readFile(String path) {
        return new byte[0];
    }

    /**
     * Not used because the call is delegated via {@link Delegate} to the {@link #workingDirectoryAccessor}.
     */
    @Override
    protected List<FileInfo> listFiles(String path) {
        return Collections.emptyList();
    }

    /**
     * Not used because the call is delegated via {@link Delegate} to the {@link #workingDirectoryAccessor}.
     */
    @Override
    protected boolean exists(String path) {
        return false;
    }

    /**
     * Not used because the call is delegated via {@link Delegate} to the {@link #workingDirectoryAccessor}.
     */
    @Override
    protected boolean isDirectory(String path) {
        return false;
    }

    /**
     * Excluded methods that Lombok will not implement.
     */
    private abstract static class ExcludedListMethods {

        public abstract List<FileInfo> listConfigurationFiles(String path);

        public abstract void writeAgentMappings(String content) throws IOException;

        public abstract void writeConfigurationFile(String file, String content) throws IOException;

        public abstract void createConfigurationDirectory(String directory) throws IOException;

        public abstract void moveConfiguration(String source, String target) throws IOException;

        public abstract void deleteConfiguration(String path) throws IOException;
    }
}
