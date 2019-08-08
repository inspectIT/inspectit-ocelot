package rocks.inspectit.oce.eum.server.beacon.extractor;

import lombok.Getter;
import rocks.inspectit.oce.eum.server.beacon.extractor.impl.DifferenceExtractor;
import rocks.inspectit.oce.eum.server.beacon.extractor.impl.SelectExtractor;

public enum BeaconFieldExtractor {

    SELECT(SelectExtractor.class),
    DIFFERENCE(DifferenceExtractor.class);

    @Getter
    private IBeaconFieldExtractor fieldExtractor;

    BeaconFieldExtractor(Class<? extends IBeaconFieldExtractor> extractorClass) {
        try {
            fieldExtractor = extractorClass.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
