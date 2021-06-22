package rocks.inspectit.ocelot.file;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * This class caches the directories of the configuration server. These directories include the current workspace, the
 * current live-revision as well as each commit. For each path which is retrieved from a directory, one entry is created.
 * Directories are cached upon first load and each entry is retained for 24 hours. The maximum amount of cached entries
 * is 10 for each directory. In case a directory has changed, the invalidate-method should be called for this directory
 * to ensure the changes are present when the directory is read again.
 */
@Component
public class DirectoryCache {

    /**
     * Timeout used for the LoadingCaches.
     */
    private final long CACHE_TIMEOUT = 86400000L; //One day as millis.

    /**
     * Size used for the LoadingCaches.
     */
    private final long CACHE_SIZE = 10;

    @Autowired
    protected FileManager fileManager;

    /**
     * LoadingCache for the working directory. The keys represent a path, the values contains a List of {@link FileInfo}
     * instances, each of which resembles a file present in the working directory under the given path,
     */
    @VisibleForTesting
    LoadingCache<String, List<FileInfo>> workingDirectoryCache;

    /**
     * LoadingCache for versioning. The keys represent the commit to be read. A special key is "live", which represents
     * the current live version. The values each are again LoadingCaches, each of which has a path as key and a List of
     * {@link FileInfo} instances, each of which resembles a file present in the versioning directory under the given path,
     */
    @VisibleForTesting
    LoadingCache<String, LoadingCache<String, List<FileInfo>>> versioningDirectoryCache;

    /**
     * Sets up the workingDirectoryCache as well as the versioningDirectoryCache.
     */
    @PostConstruct
    public void postConstruct() {
        workingDirectoryCache = generateLoadingCache("working");

        versioningDirectoryCache = CacheBuilder.newBuilder()
                .maximumSize(10)
                .expireAfterWrite(CACHE_TIMEOUT, TimeUnit.MILLISECONDS)
                .build(new CacheLoader<String, LoadingCache<String, List<FileInfo>>>() {
                    @Override
                    public LoadingCache<String, List<FileInfo>> load(String directory) {
                        return generateLoadingCache(directory);
                    }
                });
    }

    /**
     * Generates a LoadingCache based on the given directory string. Valid values for the directory String is either
     * "working", "live" or a commit id.
     * @param directory A String resembling the directory which should be used. Valid values: "working", "live" or a commit id.
     * @return A LoadingCache with the load-function set according to the directory chosen.
     */
    private LoadingCache<String, List<FileInfo>> generateLoadingCache(String directory) {
        Function<String, List<FileInfo>> listFiles = path -> fileManager.getCommitWithId(directory).listConfigurationFiles(path);

        if ("working".equals(directory)) {
            listFiles = path -> fileManager.getWorkingDirectory().listConfigurationFiles(path);
        }
        if("live".equals(directory)) {
            listFiles = path -> fileManager.getLiveRevision().listConfigurationFiles(path);
        }
        Function<String, List<FileInfo>> finalListFiles = listFiles;
        return CacheBuilder.newBuilder()
                .maximumSize(CACHE_SIZE)
                .expireAfterWrite(CACHE_TIMEOUT, TimeUnit.MILLISECONDS)
                .build(new CacheLoader<String, List<FileInfo>>() {
                    @Override
                    public List<FileInfo> load(String path) {
                        return finalListFiles.apply(path);
                    }
                });
    }

    /**
     * Invalidates the cached directory according to the given String specified. The directory will be reloaded on the
     * next invocation of get with the given directory. This method should always be called when a directory was changed.
     * The current workspace can be invalidated by setting directory to "workspace", the current live-revision can be invalidated by
     * setting directory to "live". If neither of the two applies, the String is interpreted as a commit-id and the
     * directory of the corresponding commit is invalidated.
     * @param directory A String resembling the directory to be invalidated. Valid values: "working", "live" or a commit id.
     */
    public void invalidate(String directory) {
        if ("working".equals(directory)){
            workingDirectoryCache.invalidateAll();
        } else {
            versioningDirectoryCache.invalidate(directory);
        }
    }

    /**
     * Takes a String resembling a directory and a String resembling a path. Returns a List of {@link FileInfo} instances
     * for each file found under the given path of the given directory. If the directory and the path is already cached,
     * the cached values are returned. Otherwise the directory and path is loaded and saved in the cache. The current
     * workspace can be retrieved by setting directory to "workspace", the current live-revision can be retrieved by
     * setting directory to "live". If neither of the two applies, the String is interpreted as a commit-id and the
     * directory of the corresponding commit is returned.
     * @param directory A String resembling the directory to be invalidated. Valid values: "working", "live" or a commit id.
     * @param path A String resembling the path to be loaded. Returns all files of the directory if this is empty.
     * @return A List of instances of {@link FileInfo}, each of which represent a File in the given path of the given directory.
     */
    public List<FileInfo> get(String directory, String path) throws ExecutionException {
        if ("working".equals(directory)){
            return workingDirectoryCache.get(path);
        }
        return versioningDirectoryCache.get(directory).get(path);
    }

}
