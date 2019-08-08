package rocks.inspectit.oce.eum.server.beacon.extractor.impl;

import rocks.inspectit.oce.eum.server.beacon.Beacon;
import rocks.inspectit.oce.eum.server.beacon.extractor.IBeaconFieldExtractor;
import rocks.inspectit.oce.eum.server.configuration.model.BeaconMetricDefinition;

public class DifferenceExtractor implements IBeaconFieldExtractor {

    @Override
    public Number extractValue(BeaconMetricDefinition metricDefinition, Beacon beacon) {
        String stringValueFirst = beacon.get(metricDefinition.getBeaconFields().get(0));
        String stringValueSecond = beacon.get(metricDefinition.getBeaconFields().get(1));

        switch (metricDefinition.getType()) {
            case LONG: {
                long firstValue = Long.parseLong(stringValueFirst);
                long secondValue = Long.parseLong(stringValueSecond);
                return firstValue - secondValue;
            }
            case DOUBLE: {
                double firstValue = Double.parseDouble(stringValueFirst);
                double secondValue = Double.parseDouble(stringValueSecond);
                return firstValue - secondValue;
            }
            default:
                throw new IllegalArgumentException(metricDefinition.getType().toString() + " is an unsupported metric type.");
        }
    }
}
