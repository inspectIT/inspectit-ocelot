package rocks.inspectit.oce.eum.server.beacon.extractor;

import rocks.inspectit.oce.eum.server.beacon.Beacon;
import rocks.inspectit.oce.eum.server.configuration.model.BeaconMetricDefinition;

public interface IBeaconFieldExtractor {

    Number extractValue(BeaconMetricDefinition metricDefinition, Beacon beacon);
}
