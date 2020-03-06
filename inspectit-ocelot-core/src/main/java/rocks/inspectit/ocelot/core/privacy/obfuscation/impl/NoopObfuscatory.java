package rocks.inspectit.ocelot.core.privacy.obfuscation.impl;

import io.opencensus.trace.AttributeValue;
import io.opencensus.trace.Span;
import rocks.inspectit.ocelot.core.privacy.obfuscation.IObfuscatory;

/**
 * Implementation of the {@link IObfuscatory} that does not perform any obfuscation.
 */
public class NoopObfuscatory implements IObfuscatory {

    public static final NoopObfuscatory INSTANCE = new NoopObfuscatory();

    private NoopObfuscatory() {}

}
