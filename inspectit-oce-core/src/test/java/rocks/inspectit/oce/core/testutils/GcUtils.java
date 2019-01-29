package rocks.inspectit.oce.core.testutils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

public class GcUtils {

    public static List<Object> blackhole;

    public static void waitUntilCleared(WeakReference<?> reference) {
        await().atMost(10, TimeUnit.SECONDS).until(() -> {
            System.gc();
            System.runFinalization();

            // even more trash
            for (int x = 0; x < 1000000; x++) {
                blackhole = new ArrayList<>();
            }

            Thread.sleep(10);

            return reference.get() == null;
        });
    }
}
