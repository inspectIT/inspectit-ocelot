package rocks.inspectit.ocelot.file.versioning;

/**
 * The branches which are used by the versioning manager in order to keep a modification history and a live
 * and "dirty" state of the configuration.
 */
public enum Branch {

    /**
     * The live branch. This branch is used for providing configurations to the agents.
     */
    LIVE("live"),

    /**
     * The branch which holds the modification history of the working directory.
     */
    WORKSPACE("workspace");

    /**
     * The actual branch name.
     */
    private String branchName;

    Branch(String name) {
        this.branchName = name;
    }

    public String getBranchName() {
        return branchName;
    }
}
