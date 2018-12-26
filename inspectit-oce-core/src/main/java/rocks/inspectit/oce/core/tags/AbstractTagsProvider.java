package rocks.inspectit.oce.core.tags;

import org.springframework.beans.factory.annotation.Autowired;
import rocks.inspectit.oce.core.config.model.InspectitConfig;
import rocks.inspectit.oce.core.service.DynamicallyActivatableService;

import java.util.Collections;
import java.util.Map;

public abstract class AbstractTagsProvider extends DynamicallyActivatableService implements ITagsProvider {

    @Autowired
    private CommonTagsManager commonTagsManager;

    /**
     * Internal tags.
     */
    private Map<String, String> tags = Collections.emptyMap();

    /**
     * Constructor that delegates config dependencies to the {@link DynamicallyActivatableService}.
     *
     * @param configDependencies configuration dependencies
     */
    public AbstractTagsProvider(String... configDependencies) {
        super(configDependencies);
    }

    protected abstract Map<String, String> resolveTagsInternal(InspectitConfig configuration);

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean doEnable(InspectitConfig configuration) {
        tags = resolveTagsInternal(configuration);
        commonTagsManager.register(this);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean doDisable() {
        commonTagsManager.unregister(this);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        return super.isEnabled();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> getTags() {
        return Collections.unmodifiableMap(tags);
    }

}
