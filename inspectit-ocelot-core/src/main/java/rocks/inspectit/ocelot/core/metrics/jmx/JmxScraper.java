package rocks.inspectit.ocelot.core.metrics.jmx;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;

import javax.management.*;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularType;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * <b>IMPORTANT:</b> This class was fully taken from https://github.com/prometheus/jmx_exporter v0.12 and modified.
 * <p>
 * Knows how to scrape the set of MBean servers represented by the {@link MBeanServerConnection} interface.
 */
@Slf4j
class JmxScraper {

    /**
     * Interface for collecting information about the scraped beans attribute values.
     */
    public interface MBeanReceiver {

        /**
         * @param domain          Domain of the mbean object.
         * @param beanProperties  Properties of the mbean.
         * @param attrKeys        All attribute keys
         * @param attrName        Attribute name
         * @param attrType        Attribute type
         * @param attrDescription Attribute description
         * @param value           Scraped value
         */
        void recordBean(
                String domain,
                LinkedHashMap<String, String> beanProperties,
                LinkedList<String> attrKeys,
                String attrName,
                String attrType,
                String attrDescription,
                Object value);
    }

    /**
     * Listener for the scrapings.
     */
    private final MBeanReceiver receiver;

    /**
     * Lists for whitelisting and blacklisting the mbean object names.
     *
     * @see MBeanServerConnection#queryMBeans(ObjectName, QueryExp)
     */
    private final List<ObjectName> whitelistObjectNames, blacklistObjectNames;

    /**
     * Map of cache for the jmx bean properties per mbean server.
     */
    private final Cache<MBeanServerConnection, JmxMBeanPropertyCache> jmxMBeanPropertyCacheMap = CacheBuilder.newBuilder().weakKeys().build();

    /**
     * Force the creation of the platform MBean server before first scrape.
     */
    private final boolean forcePlatformServer;

    /**
     * Default constructor.
     *
     * @param whitelistObjectNames Whitelist object names. To include all mbeans this list need to include one <code>null</code> entry.
     * @param blacklistObjectNames Blacklist object names.
     * @param receiver             Listener for the scraped values.
     * @param forcePlatformServer  Force the creation of the platform MBean server before first scrape.
     */
    public JmxScraper(List<ObjectName> whitelistObjectNames, List<ObjectName> blacklistObjectNames, MBeanReceiver receiver, boolean forcePlatformServer) {
        this.receiver = receiver;
        this.whitelistObjectNames = whitelistObjectNames;
        this.blacklistObjectNames = blacklistObjectNames;
        this.forcePlatformServer = forcePlatformServer;
    }

    /**
     * Scrapes all {@link MBeanServer}s returned by the {@link MBeanServerFactory#findMBeanServer(String)}.
     * <p>
     * Values are passed to the receiver in a single thread.
     */
    public void doScrape() throws Exception {
        // always start by forcing the platform server
        this.forcePlatformServerIfNeeded();

        ArrayList<MBeanServer> mBeanServers = MBeanServerFactory.findMBeanServer(null);
        for (MBeanServer server : mBeanServers) {
            try {
                JmxMBeanPropertyCache jmxMBeanPropertyCache = resolveJmxMBeanPropertyCache(server);
                doScrape(server, jmxMBeanPropertyCache);
            } catch (Exception e) {
                // TODO
                log.error("error scraping single server", e);
            }
        }
    }

    private JmxMBeanPropertyCache resolveJmxMBeanPropertyCache(MBeanServer server) throws ExecutionException {
        return jmxMBeanPropertyCacheMap.get(server, JmxMBeanPropertyCache::new);
    }

    /**
     * Small utility to force creation of the platform server.
     *
     * @return Platform server or null
     */
    private MBeanServer forcePlatformServerIfNeeded() {
        if (this.forcePlatformServer) {
            return ManagementFactory.getPlatformMBeanServer();
        }
        return null;
    }

    /**
     * Get a list of mbeans from the {@link MBeanServerConnection} and scrape their values.
     * <p>
     * Values are passed to the receiver in a single thread.
     */
    public void doScrape(MBeanServerConnection mBeanServerConnection, JmxMBeanPropertyCache jmxMBeanPropertyCache) throws Exception {
        // Query MBean names, see https://github.com/prometheus/jmx_exporter #89 for reasons queryMBeans() is used instead of queryNames()
        Set<ObjectName> mBeanNames = new HashSet<ObjectName>();
        for (ObjectName name : whitelistObjectNames) {
            for (ObjectInstance instance : mBeanServerConnection.queryMBeans(name, null)) {
                mBeanNames.add(instance.getObjectName());
            }
        }

        for (ObjectName name : blacklistObjectNames) {
            for (ObjectInstance instance : mBeanServerConnection.queryMBeans(name, null)) {
                mBeanNames.remove(instance.getObjectName());
            }
        }

        // Now that we have *only* the whitelisted mBeans, remove any old ones from the cache:
        jmxMBeanPropertyCache.onlyKeepMBeans(mBeanNames);

        for (ObjectName objectName : mBeanNames) {
            scrapeBean(mBeanServerConnection, objectName, jmxMBeanPropertyCache);
        }
    }

    /**
     * Scrapes one object name that belongs to the given {@link MBeanServerConnection}.
     *
     * @param beanConn
     * @param mbeanName
     * @param jmxMBeanPropertyCache
     */
    private void scrapeBean(MBeanServerConnection beanConn, ObjectName mbeanName, JmxMBeanPropertyCache jmxMBeanPropertyCache) {
        MBeanInfo info;
        try {
            info = beanConn.getMBeanInfo(mbeanName);
        } catch (IOException | JMException e) {
            logScrape(mbeanName.toString(), "getMBeanInfo Fail: " + e);
            return;
        }
        MBeanAttributeInfo[] attrInfos = info.getAttributes();

        Map<String, MBeanAttributeInfo> name2AttrInfo = new LinkedHashMap<String, MBeanAttributeInfo>();
        for (int idx = 0; idx < attrInfos.length; ++idx) {
            MBeanAttributeInfo attr = attrInfos[idx];
            if (!attr.isReadable()) {
                logScrape(mbeanName, attr, "not readable");
                continue;
            }
            name2AttrInfo.put(attr.getName(), attr);
        }
        final AttributeList attributes;
        try {
            attributes = beanConn.getAttributes(mbeanName, name2AttrInfo.keySet().toArray(new String[0]));
        } catch (Exception e) {
            logScrape(mbeanName, name2AttrInfo.keySet(), "Fail: " + e);
            return;
        }
        for (Attribute attribute : attributes.asList()) {
            MBeanAttributeInfo attr = name2AttrInfo.get(attribute.getName());
            logScrape(mbeanName, attr, "process");
            processBeanValue(
                    mbeanName.getDomain(),
                    jmxMBeanPropertyCache.getKeyPropertyList(mbeanName),
                    new LinkedList<String>(),
                    attr.getName(),
                    attr.getType(),
                    attr.getDescription(),
                    attribute.getValue()
            );
        }
    }

    /**
     * Recursive function for exporting the values of an mBean.
     * JMX is a very open technology, without any prescribed way of declaring mBeans
     * so this function tries to do a best-effort pass of getting the values/names
     * out in a way it can be processed elsewhere easily.
     */
    private void processBeanValue(
            String domain,
            LinkedHashMap<String, String> beanProperties,
            LinkedList<String> attrKeys,
            String attrName,
            String attrType,
            String attrDescription,
            Object value) {
        if (value == null) {
            logScrape(domain + beanProperties + attrName, "null");
        } else if (value instanceof Number || value instanceof String || value instanceof Boolean) {
            logScrape(domain + beanProperties + attrName, value.toString());
            this.receiver.recordBean(
                    domain,
                    beanProperties,
                    attrKeys,
                    attrName,
                    attrType,
                    attrDescription,
                    value);
        } else if (value instanceof CompositeData) {
            logScrape(domain + beanProperties + attrName, "compositedata");
            CompositeData composite = (CompositeData) value;
            CompositeType type = composite.getCompositeType();
            attrKeys = new LinkedList<String>(attrKeys);
            attrKeys.add(attrName);
            for (String key : type.keySet()) {
                String typ = type.getType(key).getTypeName();
                Object valu = composite.get(key);
                processBeanValue(
                        domain,
                        beanProperties,
                        attrKeys,
                        key,
                        typ,
                        type.getDescription(),
                        valu);
            }
        } else if (value instanceof TabularData) {
            // I don't pretend to have a good understanding of TabularData.
            // The real world usage doesn't appear to match how they were
            // meant to be used according to the docs. I've only seen them
            // used as 'key' 'value' pairs even when 'value' is itself a
            // CompositeData of multiple values.
            logScrape(domain + beanProperties + attrName, "tabulardata");
            TabularData tds = (TabularData) value;
            TabularType tt = tds.getTabularType();

            List<String> rowKeys = tt.getIndexNames();

            CompositeType type = tt.getRowType();
            Set<String> valueKeys = new TreeSet<String>(type.keySet());
            valueKeys.removeAll(rowKeys);

            LinkedList<String> extendedAttrKeys = new LinkedList<String>(attrKeys);
            extendedAttrKeys.add(attrName);
            for (Object valu : tds.values()) {
                if (valu instanceof CompositeData) {
                    CompositeData composite = (CompositeData) valu;
                    LinkedHashMap<String, String> l2s = new LinkedHashMap<String, String>(beanProperties);
                    for (String idx : rowKeys) {
                        Object obj = composite.get(idx);
                        if (obj != null) {
                            // Nested tabulardata will repeat the 'key' label, so
                            // append a suffix to distinguish each.
                            while (l2s.containsKey(idx)) {
                                idx = idx + "_";
                            }
                            l2s.put(idx, obj.toString());
                        }
                    }
                    for (String valueIdx : valueKeys) {
                        LinkedList<String> attrNames = extendedAttrKeys;
                        String typ = type.getType(valueIdx).getTypeName();
                        String name = valueIdx;
                        if (valueIdx.toLowerCase().equals("value")) {
                            // Skip appending 'value' to the name
                            attrNames = attrKeys;
                            name = attrName;
                        }
                        processBeanValue(
                                domain,
                                l2s,
                                attrNames,
                                name,
                                typ,
                                type.getDescription(),
                                composite.get(valueIdx));
                    }
                } else {
                    logScrape(domain, "not a correct tabulardata format");
                }
            }
        } else if (value.getClass().isArray()) {
            logScrape(domain, "arrays are unsupported");
        } else {
            logScrape(domain + beanProperties, attrType + " is not exported");
        }
    }

    /**
     * For debugging.
     */
    private static void logScrape(ObjectName mbeanName, Set<String> names, String msg) {
        logScrape(mbeanName + "_" + names, msg);
    }

    private static void logScrape(ObjectName mbeanName, MBeanAttributeInfo attr, String msg) {
        logScrape(mbeanName + "'_'" + attr.getName(), msg);
    }

    private static void logScrape(String name, String msg) {
        log.trace("scrape: '" + name + "': " + msg);
    }

    private static class StdoutWriter implements MBeanReceiver {
        public void recordBean(
                String domain,
                LinkedHashMap<String, String> beanProperties,
                LinkedList<String> attrKeys,
                String attrName,
                String attrType,
                String attrDescription,
                Object value) {
            System.out.println(domain +
                    beanProperties +
                    attrKeys +
                    attrName +
                    ": " + value);
        }
    }

    /**
     * Convenience function to run standalone.
     */
    public static void main(String[] args) throws Exception {
        List<ObjectName> objectNames = new LinkedList<ObjectName>();
        objectNames.add(null);
        new JmxScraper(objectNames, new LinkedList<ObjectName>(), new JmxScraper.StdoutWriter(), true).doScrape();
    }

}


