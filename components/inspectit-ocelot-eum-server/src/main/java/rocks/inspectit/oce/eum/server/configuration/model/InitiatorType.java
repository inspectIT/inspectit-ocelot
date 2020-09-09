package rocks.inspectit.oce.eum.server.configuration.model;

import rocks.inspectit.oce.eum.server.beacon.Beacon;

/**
 * Types of initiators for beacons.
 * These correspond to values for the http.initiator fields of beacons.
 */
public enum InitiatorType {
    DOCUMENT {
        @Override
        boolean hasType(String httpInitiatorValue) {
            return httpInitiatorValue == null || "".equals(httpInitiatorValue);
        }
    },
    XHR {
        @Override
        boolean hasType(String httpInitiatorValue) {
            return "xhr".equalsIgnoreCase(httpInitiatorValue);
        }
    },
    SPA_SOFT {
        @Override
        boolean hasType(String httpInitiatorValue) {
            return "spa".equalsIgnoreCase(httpInitiatorValue);
        }
    },
    SPA_HARD {
        @Override
        boolean hasType(String httpInitiatorValue) {
            return "spa_hard".equalsIgnoreCase(httpInitiatorValue);
        }
    };

    abstract boolean hasType(String httpInitiatorValue);

    public boolean hasInitiator(Beacon beacon) {
        return hasType(beacon.get("http.initiator"));
    }

}
