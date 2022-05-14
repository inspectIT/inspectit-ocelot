package rocks.inspectit.ocelot.core.opentelemetry.trace;

import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.sdk.internal.RandomSupplier;
import io.opentelemetry.sdk.trace.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.core.config.InspectitConfigChangedEvent;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;

import javax.annotation.PostConstruct;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Trace ID generator for generating 64 bit trace IDs. This class is based on the {@link io.opentelemetry.sdk.trace.RandomIdGenerator}.
 */
@Slf4j
@Component
public class CustomIdGenerator implements IdGenerator {

    @Autowired
    private InspectitEnvironment env;

    private static final long INVALID_ID = 0;

    private static final Supplier<Random> randomSupplier = RandomSupplier.platformDefault();

    private static boolean using64Bit = false;

    public static boolean isUsing64Bit() {
        return using64Bit;
    }

    @PostConstruct
    public void initialize() {
        setMode(env.getCurrentConfig().getTracing().isUse64BitTraceIds());
    }

    @EventListener
    private void configEventListener(InspectitConfigChangedEvent event) {
        boolean oldMode = event.getOldConfig().getTracing().isUse64BitTraceIds();
        boolean newMode = event.getNewConfig().getTracing().isUse64BitTraceIds();

        if (oldMode != newMode) {
            setMode(newMode);
        }
    }

    @VisibleForTesting
    void setMode(boolean is64Bit) {
        using64Bit = is64Bit;

        if (using64Bit) {
            log.info("Use of trace IDs with a length of 64 bits.");
        } else {
            log.info("Use of trace IDs with the default length (128 bits).");
        }
    }

    @Override
    public String generateSpanId() {
        Random random = randomSupplier.get();
        long id;
        do {
            id = random.nextLong();
        } while (id == INVALID_ID);

        return SpanId.fromLong(id);
    }

    @Override
    public String generateTraceId() {
        if (using64Bit) {
            return generate64BitId();
        } else {
            return generate128BitId();
        }
    }

    private String generate64BitId() {
        Random random = randomSupplier.get();
        long id;
        do {
            id = random.nextLong();
        } while (id == INVALID_ID);
        return TraceId.fromLongs(0L, id);
    }

    private String generate128BitId() {
        Random random = randomSupplier.get();
        long idHi = random.nextLong();
        long idLo;
        do {
            idLo = random.nextLong();
        } while (idLo == INVALID_ID);
        return TraceId.fromLongs(idHi, idLo);
    }

    @Override
    public String toString() {
        return "RandomIdGenerator64Bit{}";
    }
}
