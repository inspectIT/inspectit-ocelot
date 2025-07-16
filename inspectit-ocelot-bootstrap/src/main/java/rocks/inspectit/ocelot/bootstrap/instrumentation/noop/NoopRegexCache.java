package rocks.inspectit.ocelot.bootstrap.instrumentation.noop;

import rocks.inspectit.ocelot.bootstrap.exposed.RegexCache;

import java.util.regex.Pattern;

public class NoopRegexCache implements RegexCache {

    public static final NoopRegexCache INSTANCE = new NoopRegexCache();

    @Override
    public boolean matches(String regex, String toTest) {
        return false;
    }

    @Override
    public Pattern pattern(String regex) {
        return null;
    }
}
