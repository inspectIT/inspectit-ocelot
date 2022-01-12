package rocks.inspectit.oce.eum.server.rest;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import rocks.inspectit.oce.eum.server.metrics.SelfMonitoringMetricManager;
import rocks.inspectit.oce.eum.server.tracing.opentelemtry.OpenTelemetryProtoConverter;

import javax.validation.constraints.NotBlank;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController()
@Slf4j
public class TraceController {

    @Autowired
    private OpenTelemetryProtoConverter converter;

    @Autowired(required = false)
    private List<SpanExporter> spanExporters;

    @Autowired
    private SelfMonitoringMetricManager selfMonitoring;

    @CrossOrigin
    @PostMapping("spans")
    public ResponseEntity<Void> spans(@RequestBody @NotBlank String data) {
        boolean isError = false;
        Stopwatch stopwatch = Stopwatch.createStarted();
        int spanSize = -1;

        try {
            // use protobuf to convert request string to the open-telemetry proto impl
            ExportTraceServiceRequest.Builder requestBuilder = ExportTraceServiceRequest.newBuilder();
            JsonFormat.parser().merge(data, requestBuilder);
            ExportTraceServiceRequest request = requestBuilder.build();

            // then convert using our converter
            Collection<SpanData> spans = converter.convert(request);
            spanSize = spans.size();

            // export data in each exporter
            spanExporters.forEach(exporter -> exporter.export(spans));

            // return accepted
            return ResponseEntity.accepted().build();

            // in case of exception send proper response back
        } catch (InvalidProtocolBufferException e) {
            isError = true;
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OpenTelemetry data corrupted.", e);
        } catch (Exception e) {
            isError = true;
            // catch any exception in order to log
            log.warn("Exception thrown processing OpenTelemetry trace service request with post data=[{}].", data, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, null, e);
        } finally {
            stopwatch.stop();
            ImmutableMap<String, String> tagMap = ImmutableMap.of("is_error", String.valueOf(isError));
            selfMonitoring.record("traces_received", stopwatch.elapsed(TimeUnit.MILLISECONDS), tagMap);
            if (spanSize >= 0) {
                selfMonitoring.record("traces_span_size", spanSize, tagMap);
            }
        }
    }

}
