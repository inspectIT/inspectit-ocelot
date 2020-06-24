package rocks.inspectit.ocelot.file.versioning;

public enum Branch {

    LIVE("live"),

    WORKSPACE("workspace");

    private String branchName;

    Branch(String name) {
        this.branchName = name;
    }

    public String getBranchName() {
        return branchName;
    }
}
