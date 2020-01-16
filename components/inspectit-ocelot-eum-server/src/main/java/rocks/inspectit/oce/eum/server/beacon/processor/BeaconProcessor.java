package rocks.inspectit.oce.eum.server.beacon.processor;

import rocks.inspectit.oce.eum.server.beacon.Beacon;

/**
 * Interface for all components acting as {@link BeaconProcessor}.
 * BeaconProcessors are intended to enrich a Beacon with new values.
 */
public interface BeaconProcessor {

    /**
     * @param beacon The {@link Beacon} to be processed
     * @return A new {@link Beacon} instance
     */
    Beacon process(Beacon beacon);
}