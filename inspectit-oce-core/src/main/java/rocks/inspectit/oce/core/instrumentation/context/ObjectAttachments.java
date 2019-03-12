package rocks.inspectit.oce.core.instrumentation.context;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import rocks.inspectit.oce.bootstrap.instrumentation.IObjectAttachments;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation for the bootstrap interface {@link IObjectAttachments}
 */
@Slf4j
public class ObjectAttachments implements IObjectAttachments {

    /**
     * The name of this bean, initialized via the {@link rocks.inspectit.oce.core.config.spring.BootstrapInitializerConfiguration}
     */
    public static final String BEAN_NAME = "objectAttachments";

    private final Cache<Object, ConcurrentHashMap<String, Object>> attachments = CacheBuilder.newBuilder()
            .weakKeys()
            .build();

    @Override
    public void attach(Object target, String key, Object value) {
        if (target != null) {
            try {
                ConcurrentHashMap<String, Object> map = attachments.get(target, ConcurrentHashMap::new);
                if (value != null) {
                    map.put(key, value);
                } else {
                    map.remove(key);
                }
            } catch (Exception e) {
                log.error("Error storing value", e);
            }
        }
    }

    @Override
    public Object getAttachment(Object target, String key) {
        val objAttachments = attachments.getIfPresent(target);
        if (objAttachments != null) {
            return objAttachments.get(key);
        } else {
            return null;
        }
    }
}
