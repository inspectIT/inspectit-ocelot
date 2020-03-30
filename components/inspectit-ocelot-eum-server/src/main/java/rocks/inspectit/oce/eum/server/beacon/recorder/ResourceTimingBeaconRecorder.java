package rocks.inspectit.oce.eum.server.beacon.recorder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opencensus.common.Scope;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import rocks.inspectit.oce.eum.server.beacon.Beacon;
import rocks.inspectit.oce.eum.server.metrics.MeasuresAndViewsManager;
import rocks.inspectit.ocelot.config.model.metrics.definition.MetricDefinitionSettings;
import rocks.inspectit.ocelot.config.model.metrics.definition.ViewDefinitionSettings;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

/**
 * This {@link BeaconRecorder} processes plain resource timing entry from the {@link Beacon} and exposes metric that:
 * <ul>
 *     <li>reports number of resources loaded sliced by type, cross-origin and cached tags</li>
 * </ul>
 * <p>
 * The impl depends heavily on the Boomerang compression of the resource timing entries in the beacon. Please read
 * <a href="https://developer.akamai.com/tools/boomerang/docs/BOOMR.plugins.ResourceTiming.html">ResourceTiming</a>
 * Boomerang documentation first.
 */
@Component
@ConditionalOnProperty(value = "inspectit-eum-server.resource-timing.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class ResourceTimingBeaconRecorder implements BeaconRecorder {

    /**
     * Metric definition for the resource timing total metric.
     */
    private static final MetricDefinitionSettings RESOURCE_TIMING_TOTAL;

    static {
        Map<String, Boolean> tags = new HashMap<>();
        tags.put("initiatorType", true);
        tags.put("cached", true);
        tags.put("crossOrigin", true);

        RESOURCE_TIMING_TOTAL = MetricDefinitionSettings.builder()
                .type(MetricDefinitionSettings.MeasureType.LONG)
                .unit("ms")
                .view("resource_timing_total/COUNT", ViewDefinitionSettings.builder()
                        .tags(tags)
                        .aggregation(ViewDefinitionSettings.Aggregation.COUNT)
                        .build()
                )
                .build();
    }

    /**
     * Object mapper used to read the resource timing
     */
    @Autowired
    private final ObjectMapper objectMapper;

    /**
     * {@link MeasuresAndViewsManager} for exposing metrics.
     */
    @Autowired
    private final MeasuresAndViewsManager measuresAndViewsManager;

    /**
     * Init metric(s).
     */
    @PostConstruct
    public void initMetric() {
        measuresAndViewsManager.updateMetrics("resource_timing_total", RESOURCE_TIMING_TOTAL);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Parses the <code>restiming</code> entry from the beacon and exposes metric(s) about resource timing if parsing is
     * success.
     */
    @Override
    public void record(Beacon beacon) {
        // this is the URL where the resources have been loaded
        String url = beacon.get("u");

        Optional.ofNullable(beacon.get("restiming"))
                .ifPresent(resourceTiming -> this.resourceTimingResults(resourceTiming)
                        .forEach(rs -> this.record(rs, url))
                );
    }

    /**
     * Records one {@link ResourceTimingEntry} to the exposed metric(s).
     *
     * @param resourceTimingEntry entry
     * @param url                 URL of the page where the resource has been loaded from.
     */
    private void record(ResourceTimingEntry resourceTimingEntry, String url) {
        Map<String, String> extra = new HashMap<>();
        boolean sameOrigin = isSameOrigin(url, resourceTimingEntry.url);
        extra.put("crossOrigin", String.valueOf(!sameOrigin));
        extra.put("initiatorType", resourceTimingEntry.getInitiatorType().toString());
        // TODO is this OK, only setting cached if it's same origin, otherwise we can not know
        if (sameOrigin) {
            extra.put("cached", String.valueOf(resourceTimingEntry.isCached(true)));
        }

        try (Scope scope = measuresAndViewsManager.getTagContext(extra).buildScoped()) {
            // TODO I think we should already collect there the load time, thus we would have a COUNT and a SUM of time
            //  we would have most of the stuff needed then
            //  and it would make tests more reliable then now
            measuresAndViewsManager.recordMeasure("resource_timing_total", RESOURCE_TIMING_TOTAL, 1L);
        }
    }

    /**
     * Takes Boomerang resource timing JSON and returns stream of found {@link ResourceTimingEntry}s.
     *
     * @param resourceTiming json
     * @return stream
     */
    private Stream<ResourceTimingEntry> resourceTimingResults(String resourceTiming) {
        JsonNode rootNode;
        try {
            rootNode = objectMapper.readTree(resourceTiming);
        } catch (IOException e) {
            log.error("Error converting resource timing json to tree.", e);
            return Stream.empty();
        }

        return findAllTimingValuesAsText(rootNode).entrySet()
                .stream()
                .flatMap(entry -> this.resolveResourceTimingStringValue(entry.getKey(), entry.getValue()));

    }

    /**
     * Helper to construct map of resource URLs to resource timing details.
     *
     * @param node root node
     * @return Map where keys are resource URLs and values are the Boomerang compressed resource timing string.
     */
    private Map<String, String> findAllTimingValuesAsText(JsonNode node) {
        Map<String, String> map = new HashMap<>();
        this.findAllTimingValuesAsText(node, map, "");
        return map;
    }

    private void findAllTimingValuesAsText(JsonNode node, Map<String, String> foundSoFar, String prefix) {
        if (node.isValueNode()) {
            if (node.isTextual()) {
                foundSoFar.put(prefix, node.textValue());
            }
        } else {
            node.fields().forEachRemaining(entry -> this.findAllTimingValuesAsText(entry.getValue(), foundSoFar, prefix + entry.getKey()));
        }

    }

    /**
     * Maps one resource URL and it's timing given as the Boomerang compressed string to the stream of {@link ResourceTimingEntry}.
     * <p>
     * Note that one compressed string can contain multiple loads of the same entry. This method ignores any additional
     * data (transfer size, image size, etc) from the Boomerang string.
     *
     * @param url   URL of the loaded resource.
     * @param value Boomerang compressed resource timing string for a single entry
     * @return
     */
    private Stream<ResourceTimingEntry> resolveResourceTimingStringValue(String url, String value) {
        return Stream.of(value)
                // split by pipe, as pipe separates same resource load times if executed more than once
                .flatMap(possibleMultipleValue -> {
                    String[] split = possibleMultipleValue.split("\\|");
                    return Arrays.stream(split);
                })
                .flatMap(singeValue -> ResourceTimingEntry.from(url, singeValue).map(Stream::of).orElseGet(Stream::empty));
    }

    /**
     * Checks if two URLs are considered as same origin. Based on {@link org.springframework.web.util.WebUtils#isSameOrigin(HttpRequest)}.
     *
     * @param u1 first url
     * @param u2 second url
     * @return Returns true if two urls are considered as same-origin
     * @see org.springframework.web.util.WebUtils#isSameOrigin(HttpRequest)
     * @see 'https://developer.mozilla.org/en-US/docs/Web/Security/Same-origin_policy'
     */
    private static boolean isSameOrigin(String u1, String u2) {
        UriComponents uriComponents1 = UriComponentsBuilder.fromUriString(u1).build();
        UriComponents uriComponents2 = UriComponentsBuilder.fromUriString(u2).build();

        return Objects.equals(uriComponents1.getScheme(), uriComponents2.getScheme()) &&
                Objects.equals(uriComponents1.getHost(), uriComponents2.getHost()) &&
                getPort(uriComponents1.getScheme(), uriComponents1.getPort()) == getPort(uriComponents2.getScheme(), uriComponents2.getPort());
    }

    private static int getPort(@Nullable String scheme, int port) {
        if (port == -1) {
            if ("http".equals(scheme) || "ws".equals(scheme)) {
                port = 80;
            } else if ("https".equals(scheme) || "wss".equals(scheme)) {
                port = 443;
            }
        }
        return port;
    }

    @Value
    @Builder
    public static class ResourceTimingEntry {

        /**
         * Url of the resource.
         */
        String url;

        /**
         * Initiator type.
         */
        InitiatorType initiatorType;

        /**
         * Timings array in following order:
         * <br>
         * <code>timings = "[startTime],[responseEnd],[responseStart],[requestStart],[connectEnd],[secureConnectionStart],[connectStart],[domainLookupEnd],[domainLookupStart],[redirectEnd],[redirectStart]"</code>
         * <p>
         * Note that array does not need to be fully populated if entries from the end are missing.
         */
        Integer[] timings;

        /**
         * Cached resources only have 2 timing values if considered as same-origin requests.
         *
         * @param sameOrigin If this is considered as same origin resource loading
         * @return If cached or not.
         */
        public boolean isCached(boolean sameOrigin) {
            if (this.timings == null || this.timings.length < 3) {
                return sameOrigin;
            }
            return false;
        }

        /**
         * Constructs the {@link ResourceTimingEntry} from a single (non-piped) string value.
         * <p>
         * Will resolve to empty if it contains only additional data.
         *
         * @return ResourceTimingEntry as optional
         */
        public static Optional<ResourceTimingEntry> from(String url, String value) {
            // check if this string contains additional data
            // if so cut it from processing
            String toProcess = value;
            int additionalDataIndex = value.indexOf('*');
            if (additionalDataIndex > -1) {
                toProcess = value.substring(0, additionalDataIndex);
            }

            if (StringUtils.isEmpty(toProcess)) {
                return Optional.empty();
            }

            // initiator is always first char
            InitiatorType initiatorType = InitiatorType.from(toProcess.charAt(0));

            // then split by comma to get all timings
            String[] timingsAsBase36Strings = toProcess.substring(1).split(",");

            // then convert timings in base36 to int values
            // if empty then it's zero
            Integer[] timings = Arrays.stream(timingsAsBase36Strings)
                    .map(v -> StringUtils.isEmpty(v) ? 0 : Integer.parseInt(v, 36))
                    .toArray(Integer[]::new);

            // then build the entry
            return Optional.of(ResourceTimingEntry.builder()
                    .url(url)
                    .initiatorType((initiatorType))
                    .timings(timings)
                    .build()
            );
        }

    }

    /**
     * Initiator type represented by a char.
     *
     * @see 'https://developer.akamai.com/tools/boomerang/docs/BOOMR.plugins.ResourceTiming.html'
     */
    public enum InitiatorType {
        OTHER('0'),
        IMG('1'),
        LINK('2'),
        SCRIPT('3'),
        CSS('4'),
        XML_HTTP_REQUEST('5'),
        HTML('6'),
        IMAGE('7'),
        BEACON('8'),
        FETCH('9'),
        IFRAME('a'),
        BODY('b'),
        INPUT('c'),
        OBJECT('d'),
        VIDEO('e'),
        AUDIO('f'),
        SOURCE('g'),
        TRACK('h'),
        EMBED('i'),
        EVENT_SOURCE('j');

        private char identifier;

        InitiatorType(char identifier) {
            this.identifier = identifier;
        }

        /**
         * Returns the {@link InitiatorType} represented by this char. If not found {@link #OTHER} is returned.
         *
         * @param c identifier
         * @return {@link InitiatorType}
         */
        public static InitiatorType from(char c) {
            return Arrays.stream(InitiatorType.values())
                    .filter(initiatorType -> initiatorType.identifier == c)
                    .findFirst()
                    .orElse(OTHER);
        }

    }
}
