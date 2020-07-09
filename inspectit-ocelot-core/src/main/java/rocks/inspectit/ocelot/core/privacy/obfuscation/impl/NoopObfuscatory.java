package rocks.inspectit.ocelot.core.privacy.obfuscation.impl;

import rocks.inspectit.ocelot.core.privacy.obfuscation.IObfuscatory;

/**
 * Implementation of the {@link IObfuscatory} that does not perform any obfuscation.
 */
public class NoopObfuscatory implements IObfuscatory {

    public static final NoopObfuscatory INSTANCE = new NoopObfuscatory();

    private NoopObfuscatory() {
    }

}
