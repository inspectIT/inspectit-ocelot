package rocks.inspectit.oce.eum.server.beacon.extractor.impl;

import rocks.inspectit.oce.eum.server.beacon.Beacon;
import rocks.inspectit.oce.eum.server.beacon.extractor.IBeaconFieldExtractor;
import rocks.inspectit.oce.eum.server.configuration.model.BeaconMetricDefinition;

public class SelectExtractor implements IBeaconFieldExtractor {

    @Override
    public Number extractValue(BeaconMetricDefinition metricDefinition, Beacon beacon) {
        String targetField = metricDefinition.getBeaconFields().get(0);
        String stringValue = beacon.get(targetField);

        switch (metricDefinition.getType()) {
            case LONG:
                return Long.parseLong(stringValue);
            case DOUBLE:
                return Double.parseDouble(stringValue);
            default:
                throw new IllegalArgumentException(metricDefinition.getType().toString() + " is an unsupported metric type.");
        }
    }
}
