package rocks.inspectit.ocelot.core.opentelemetry.trace;

import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.api.internal.OtelEncodingUtils;
import io.opentelemetry.api.internal.TemporaryBuffers;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.sdk.internal.RandomSupplier;
import io.opentelemetry.sdk.trace.IdGenerator;

import java.util.Random;
import java.util.function.Supplier;

import static io.opentelemetry.api.internal.OtelEncodingUtils.byteToBase16;

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

    private String generateId(){
        Random random = randomSupplier.get();
        long idHi = random.nextLong();
        long idLo;
        do {
            idLo = random.nextLong();
        } while (idLo == INVALID_ID);

        if (idHi == 0 && idLo == 0) {
            return INVALID_STRING_ID;
        }
        char[] chars = TemporaryBuffers.chars(32);
        OtelEncodingUtils.longToBase16String(idHi, chars, 0);
        OtelEncodingUtils.longToBase16String(idLo, chars, 16);
        return new String(chars, 0, 8);
    }

    @Override
    public String toString() {
        return "RandomIdGenerator{}";
    }
}
