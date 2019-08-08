package rocks.inspectit.oce.eum.server.utils;

public enum DefaultTags {

    COUNTRY_CODE;

    public static boolean isDefaultTag(String tag) {
        for (DefaultTags defaultTag : DefaultTags.values()) {
            if (defaultTag.name().equalsIgnoreCase(tag)) {
                return true;
            }
        }
        return false;
    }
}

