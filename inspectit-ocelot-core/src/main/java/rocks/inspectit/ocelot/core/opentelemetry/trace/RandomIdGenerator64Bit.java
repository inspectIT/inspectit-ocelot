package rocks.inspectit.ocelot.core.opentelemetry.trace;

import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.api.internal.OtelEncodingUtils;
import io.opentelemetry.api.internal.TemporaryBuffers;
import io.opentelemetry.sdk.internal.RandomSupplier;
import io.opentelemetry.sdk.trace.IdGenerator;

import java.util.Random;
import java.util.function.Supplier;

/**
 * Trace ID generator for generating 64 bit trace IDs. This class is based on the {@link io.opentelemetry.sdk.trace.RandomIdGenerator}.
 */
public enum RandomIdGenerator64Bit implements IdGenerator {
    INSTANCE;

    private static final long INVALID_ID = 0;

    @VisibleForTesting
    static final String INVALID_STRING_ID = "00000000";

    private static final Supplier<Random> randomSupplier = RandomSupplier.platformDefault();

    @Override
    public String generateSpanId() {
        return generateId();
    }

    @Override
    public String generateTraceId() {
        return generateId();
    }

    private String generateId() {
        Random random = randomSupplier.get();
        long id;
        do {
            id = random.nextLong();
        } while (id == INVALID_ID);

        char[] chars = TemporaryBuffers.chars(16);
        OtelEncodingUtils.longToBase16String(id, chars, 0);
        return new String(chars, 0, 8);
    }

    @Override
    public String toString() {
        return "RandomIdGenerator64Bit{}";
    }
}
