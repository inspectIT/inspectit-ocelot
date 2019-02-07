package rocks.inspectit.oce.core.instrumentation.config.matcher;

import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationSource;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.CollectionItemMatcher;
import net.bytebuddy.matcher.DeclaringAnnotationMatcher;
import net.bytebuddy.matcher.ElementMatcher;

import static net.bytebuddy.matcher.ElementMatchers.annotationType;

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
            log.warn("Exception during annotation matching. The target will be considered as 'not matching'. {}: {}", target, e.getMessage());
            return false;
        }
    }

}
