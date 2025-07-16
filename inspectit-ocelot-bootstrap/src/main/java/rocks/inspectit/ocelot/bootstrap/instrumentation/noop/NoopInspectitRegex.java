package rocks.inspectit.ocelot.bootstrap.instrumentation.noop;

import rocks.inspectit.ocelot.bootstrap.exposed.InspectitRegex;

import java.util.regex.Pattern;

public class NoopInspectitRegex implements InspectitRegex {

    public static final NoopInspectitRegex INSTANCE = new NoopInspectitRegex();

    @Override
    public boolean matches(String regex, String toTest) {
        return false;
    }

    @Override
    public Pattern pattern(String regex) {
        return null;
    }
}
