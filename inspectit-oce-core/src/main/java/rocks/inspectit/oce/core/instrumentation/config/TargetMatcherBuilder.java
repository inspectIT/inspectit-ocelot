package rocks.inspectit.oce.core.instrumentation.config;

import net.bytebuddy.matcher.ElementMatcher;

import static net.bytebuddy.matcher.ElementMatchers.not;

public class TargetMatcherBuilder<T> {

    private ElementMatcher.Junction<T> matcher = null;

    private ElementMatcher.Junction<T> fullMatcher = null;

    public ElementMatcher.Junction<T> build() {
        if (matcher != null) {
            return fullMatcher.or(matcher);
        }
        return fullMatcher;
    }

    public void next() {
        if (matcher != null) {
            if (fullMatcher == null) {
                fullMatcher = matcher;
                matcher = null;
            } else {
                fullMatcher = fullMatcher.or(matcher);
                matcher = null;
            }
        }
    }

    public void and(ElementMatcher.Junction<T> nextMatcher) {
        if (matcher == null) {
            matcher = nextMatcher;
        } else {
            matcher = matcher.and(nextMatcher);
        }
    }

    public void and(Boolean condition, ElementMatcher.Junction<T> nextMatcher) {
        if (condition != null) {
            if (condition) {
                and(nextMatcher);
            } else {
                and(not(nextMatcher));
            }
        }
    }
}
