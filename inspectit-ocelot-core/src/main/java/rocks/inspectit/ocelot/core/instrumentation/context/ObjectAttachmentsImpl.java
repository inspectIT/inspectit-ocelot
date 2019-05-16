package rocks.inspectit.ocelot.core.instrumentation.context;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import rocks.inspectit.ocelot.bootstrap.exposed.ObjectAttachments;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation for the bootstrap interface {@link ObjectAttachments}
 */
@Slf4j
public class ObjectAttachmentsImpl implements ObjectAttachments {

    /**
     * The name of this bean, initialized via the {@link rocks.inspectit.ocelot.core.config.spring.BootstrapInitializerConfiguration}
     */
    public static final String BEAN_NAME = "objectAttachments";

    private final Cache<Object, ConcurrentHashMap<String, Object>> attachments = CacheBuilder.newBuilder()
            .weakKeys()
            .build();

    @Override
    public Object attach(Object target, String key, Object value) {
        Object previous = null;
        if (target != null) {
            try {
                ConcurrentHashMap<String, Object> map = attachments.get(target, ConcurrentHashMap::new);
                if (value != null) {
                    previous = map.put(key, value);
                } else {
                    previous = map.remove(key);
                }
            } catch (Exception e) {
                log.error("Error storing value", e);
            }
        }
        return previous;
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
