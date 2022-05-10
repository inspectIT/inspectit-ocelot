package rocks.inspectit.ocelot.core.opentelemetry.trace;

import io.opentelemetry.api.internal.OtelEncodingUtils;
import io.opentelemetry.api.internal.TemporaryBuffers;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.sdk.internal.RandomSupplier;
import io.opentelemetry.sdk.trace.IdGenerator;

import java.util.Random;
import java.util.function.Supplier;

public enum RandomIdGenerator64Bit implements IdGenerator {
    INSTANCE;

    private static final long INVALID_ID = 0;

    private static final String INVALID_STRING_ID = "00000000";
    private static final Supplier<Random> randomSupplier = RandomSupplier.platformDefault();

    @Override
    public String generateSpanId() {
        long id;
        Random random = randomSupplier.get();
        do {
            id = random.nextLong() / 2;
        } while (id == INVALID_ID);
        return SpanId.fromLong(id);
    }

    @Override
    public String generateTraceId() {
        Random random = randomSupplier.get();
        long idHi = random.nextLong();
        long idLo;
        do {
            idLo = random.nextLong();
        } while (idLo == INVALID_ID);

        if (idHi == 0 && idLo == 0) {
            return INVALID_STRING_ID;
        }
        char[] chars = TemporaryBuffers.chars(16);
        OtelEncodingUtils.longToBase16String(idHi, chars, 0);
        OtelEncodingUtils.longToBase16String(idLo, chars, 16);
        return new String(chars, 0, 16);
    }

    @Override
    public String toString() {
        return "RandomIdGenerator{}";
    }
}
