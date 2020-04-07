package rocks.inspectit.oce.eum.server.beacon.recorder;

import rocks.inspectit.oce.eum.server.beacon.Beacon;

/**
 * Interface for all components acting as {@link BeaconRecorder}.
 * BeaconRecorder are intended to record custom complicated metrics from a fully-processed Beacon.
 */
public interface BeaconRecorder {

    /**
     * Records arbitrary metrics from given the {@link Beacon}. This method will be invoked within the scope where
     * global tags are already added.
     *
     * @param beacon Fully-processed beacon
     */
    void record(Beacon beacon);

}
