package rocks.inspectit.ocelot.autocomplete.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.events.WorkspaceChangedEvent;
import rocks.inspectit.ocelot.file.FileManager;
import rocks.inspectit.ocelot.file.accessor.git.RevisionAccess;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutorService;

/**
 * Caches the workspace revision.
 */
@Slf4j
@Component
public class ConfigurationFilesCache {

    @Autowired
    private FileManager fileManager;

    @Autowired
    private ExecutorService executor;

    /**
     * The currently active task for reloading the configuration.
     */
    private ConfigurationFilesCacheReloadTask activeReloadTask;

    /**
     * The current parsed contents of all configuration files.
     */
    private Collection<Object> parsedContents = Collections.emptyList();

    /**
     * Returns the most recently loaded .yaml and .yml files as a list of Objects. Each Object resembles the corresponding
     * files root element. All following elements are then appended to this root element.
     * The objects are either nested Lists or Maps.
     * e.g.: the file x.yaml is loaded with the content
     * root:<br>
     * listOne:<br>
     * - valueOne<br>
     * - valueTwo<br>
     * setOne:<br>
     * valueThree
     * In the returned list there would be the Map "root" containing a key "root", which contains Map containing
     * a key "listOne" with a list containing the elements "valueOne" and "valueTwo". Element "setOne" would only contain
     * the value "valueThree".
     *
     * @return A Collection containing all loaded .yaml and .yml files root elements as Maps or Lists.
     */
    public Collection<Object> getParsedContents() {
        return parsedContents;
    }

    /**
     * Loads all .yaml and .yml files. The files are loaded from the "configuration" folder of the server and from the
     * "files" folder of the working directory. The files contents are parsed into either nested Lists or Maps.
     */
    @PostConstruct
    @EventListener(WorkspaceChangedEvent.class)
    public synchronized void loadFiles() {
        RevisionAccess fileAccess = fileManager.getWorkspaceRevision();
        if (activeReloadTask != null) {
            activeReloadTask.cancel();
        }
        activeReloadTask = new ConfigurationFilesCacheReloadTask(fileAccess, (configs) -> parsedContents = configs);
        executor.submit(activeReloadTask);
    }

}
