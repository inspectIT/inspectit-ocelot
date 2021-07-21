package rocks.inspectit.ocelot.file.versioning;

/**
 * The result of a configuration promotion.
 */
public enum PromotionResult {

    /**
     * Everything was OK.
     */
    OK,

    /**
     * The promotion was successful but the remote synchronization failed - e.g. because of fast-forward rejection.
     */
    SYNCHRONIZATION_FAILED;
}
