package rocks.inspectit.ocelot.core.metrics.percentiles;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class WindowedDoubleQueuePerfTest {

    /**
     * Inserts 10 mio points into teh queue at a rate of (simulated) 1000 points per second.
     * Because the queue covers a window of 100 seconds, is has a peak size of 100k points.
     * <p>
     * This test keeps the rate of points constant, meaning that queue does not need to resize.
     */
    @Benchmark
    public void bestCase(Blackhole blackhole) {
        WindowedDoubleQueue queue = new WindowedDoubleQueue(100 * 1000);
        insertPoints(queue, 0, 1000, 10 * 1000 * 1000); //insert 10 mio points in total
        blackhole.consume(queue);
    }

    /**
     * Inserts 10 mio points into at a rate of (simulated) 1000 points per second.
     * Because the queue covers a window of 100 seconds, is has a peak size of 100k points.
     * <p>
     * This test inserts the points with gaps in time, causing the queue to reset
     * and grow again, causing memory allocation, deallocation and data copying.
     */
    @Benchmark
    public void worstCase(Blackhole blackhole) {
        WindowedDoubleQueue queue = new WindowedDoubleQueue(100 * 1000);
        for (int i = 0; i < 100; i++) {
            insertPoints(queue, 1000L * 1000L * i, 1000, 100 * 1000);
        }
        blackhole.consume(queue);
    }

    void insertPoints(WindowedDoubleQueue queue, long startTime, double pointsPerSecond, long duration) {
        double msPerPoint = 1000.0 / pointsPerSecond;
        double time = startTime;
        while ((time - startTime) < duration) {
            queue.insert(time, (long) time);
            time += msPerPoint;
        }
    }
}
