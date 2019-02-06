package rocks.inspectit.oce.core.instrumentation.config.matcher;

import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.CollectionItemMatcher;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.matcher.InheritedAnnotationMatcher;

/**
 * Matcher which is equal to {@link ElementMatchers#hasAnnotation(ElementMatcher)} but will catch
 * Exception in case any is thrown.
 * <p>
 * An Exception can occur using the ElementMatchers#hasAnnotation because the {@link Class#getAnnotations()} is used which may throw an
 * exception if an annotation cannot be resolved (e.g. missing dependencies).
 */
@HashCodeAndEqualsPlugin.Enhance
@Slf4j
public class WrappedHasAnnotationMatcher<T extends TypeDescription> extends InheritedAnnotationMatcher<T> {

    /**
     * Creates a new matcher for the inherited annotations of a type description.
     */
    public static <T extends TypeDescription> WrappedHasAnnotationMatcher<T> of(ElementMatcher<? super AnnotationDescription> matcher) {
        return new WrappedHasAnnotationMatcher<>(new CollectionItemMatcher<>(matcher));
    }

    private WrappedHasAnnotationMatcher(CollectionItemMatcher<AnnotationDescription> matcher) {
        super(matcher);
    }

    @Override
    public boolean matches(T target) {
        try {
            return super.matches(target);
        } catch (Exception e) {
            log.warn("Exception during annotation matching of {}: {}", target, e.getMessage());
            return false;
        }
    }
}
