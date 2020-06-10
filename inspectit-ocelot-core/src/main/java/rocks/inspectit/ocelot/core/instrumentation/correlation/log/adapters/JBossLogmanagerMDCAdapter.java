package rocks.inspectit.ocelot.core.instrumentation.correlation.log.adapters;

import rocks.inspectit.ocelot.core.utils.WeakMethodReference;

/**
 * Provides access to JBoss Logmanagers ThreadContext.
 * @author boris_unckel
 *
 */
public class JBossLogmanagerMDCAdapter extends AbstractStaticMapMDCAdapter {

	/**
     * The name of the MDC class of JBoss Logmanager.
     */
    public static final String MDC_CLASS = "org.jboss.logmanager.MDC";
    

    private JBossLogmanagerMDCAdapter(WeakMethodReference put, WeakMethodReference get, WeakMethodReference remove) {
        super(put, get, remove);
    }
    
    /**
     * Creates an Adapater given a ThreadContext class.
     *
     * @param mdcClazz the org.jboss.logmanager.MDC class
     * @return and adapter for setting values on the given thread context.
     */
    public static JBossLogmanagerMDCAdapter get(Class<?> mdcClazz) {
        try {
            WeakMethodReference put = WeakMethodReference.create(mdcClazz, "put", String.class, String.class);
            WeakMethodReference get = WeakMethodReference.create(mdcClazz, "get", String.class);
            WeakMethodReference remove = WeakMethodReference.create(mdcClazz, "remove", String.class);
            return new JBossLogmanagerMDCAdapter(put, get, remove);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("ThreadContext class did not contain expected methods", e);
        }
    }
}
