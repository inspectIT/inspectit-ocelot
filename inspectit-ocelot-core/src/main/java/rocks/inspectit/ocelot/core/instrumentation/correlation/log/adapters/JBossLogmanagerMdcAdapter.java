package rocks.inspectit.ocelot.core.instrumentation.correlation.log.adapters;

import rocks.inspectit.ocelot.config.model.tracing.TraceIdMDCInjectionSettings;
import rocks.inspectit.ocelot.core.utils.WeakMethodReference;

import java.lang.reflect.Method;

/**
 * Provides access to JBoss Logmanagers ThreadContext.
 *
 * @author boris_unckel
 */
public class JBossLogmanagerMdcAdapter extends AbstractStaticMapMDCAdapter {

    /**
     * The name of the MDC class of JBoss Logmanager.
     */
    public static final String MDC_CLASS = "org.jboss.logmanager.MDC";

    private JBossLogmanagerMdcAdapter(WeakMethodReference put, WeakMethodReference get, WeakMethodReference remove) {
        super(put, get, remove);
    }

    public JBossLogmanagerMdcAdapter() {
    }

    /**
     * Creates an Adapater given a JBoss Logmanager MDC class.
     *
     * @param mdcClazz the org.jboss.logmanager.MDC class
     *
     * @return an adapter for setting values on the given MDC.
     */
    public static JBossLogmanagerMdcAdapter get(Class<?> mdcClazz) {
        try {
            WeakMethodReference put = WeakMethodReference.create(mdcClazz, "put", String.class, String.class);
            WeakMethodReference get = WeakMethodReference.create(mdcClazz, "get", String.class);
            WeakMethodReference remove = WeakMethodReference.create(mdcClazz, "remove", String.class);
            return new JBossLogmanagerMdcAdapter(put, get, remove);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("JBoss MDC class did not contain expected methods", e);
        }
    }

    @Override
    public String getMDCClassName() {
        return "org.jboss.logmanager.MDC";
    }

    @Override
    public Method getGetMethod(Class<?> mdcClass) throws NoSuchMethodException {
        return mdcClass.getMethod("get", String.class);
    }

    @Override
    public Method getPutMethod(Class<?> mdcClass) throws NoSuchMethodException {
        return mdcClass.getMethod("put", String.class, String.class);
    }

    @Override
    public Method getRemoveMethod(Class<?> mdcClass) throws NoSuchMethodException {
        return mdcClass.getMethod("remove", String.class);
    }

    @Override
    public boolean isEnabledForConfig(TraceIdMDCInjectionSettings settings) {
        return settings.isJbossLogmanagerEnabled();
    }
}