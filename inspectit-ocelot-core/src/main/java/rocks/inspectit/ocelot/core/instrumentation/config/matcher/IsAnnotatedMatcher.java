package rocks.inspectit.ocelot.core.instrumentation.config.matcher;

import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationSource;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.CollectionItemMatcher;
import net.bytebuddy.matcher.DeclaringAnnotationMatcher;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

import static net.bytebuddy.matcher.ElementMatchers.annotationType;

/**
 * This class represents an {@link ElementMatcher} equal to {@link ElementMatchers#isAnnotatedWith(ElementMatcher)}
 * but catching any exceptions which occur during the matching process (calling the {@link #matches(AnnotationSource)} method.
 */
@HashCodeAndEqualsPlugin.Enhance
@Slf4j
public class IsAnnotatedMatcher<N extends AnnotationSource> extends DeclaringAnnotationMatcher<N> {

    /**
     * Creates a new matcher for the inherited annotations of a type description.
     */
    public static <T extends AnnotationSource> ElementMatcher.Junction<T> of(ElementMatcher<? super TypeDescription> matcher) {
        if (matcher == null) {
            return null;
        }
        return new IsAnnotatedMatcher<>(new CollectionItemMatcher<>(annotationType(matcher)));
    }

    private IsAnnotatedMatcher(CollectionItemMatcher<AnnotationDescription> matcher) {
        super(matcher);
    }

    @Override
    public boolean matches(N target) {
        try {
            return super.matches(target);
        } catch (Exception e) {
            log.warn("Exception during annotation matching. The target will be considered as 'not matching'. {}: {}", target, e
                    .getMessage());
            return false;
        }
    }

}
