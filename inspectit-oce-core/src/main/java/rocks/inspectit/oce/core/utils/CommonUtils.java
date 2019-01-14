package rocks.inspectit.oce.core.utils;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

/**
 * A generic utility class.
 */
public class CommonUtils {

    public static boolean isJVMShuttingDown() {
        Thread dummyHook = new Thread(() -> {
        });
        try {
            Runtime.getRuntime().addShutdownHook(dummyHook);
            Runtime.getRuntime().removeShutdownHook(dummyHook);
        } catch (IllegalStateException e) {
            return true;
        }
        return false;
    }

    /**
     * Tries to get (and NOT remove) a single element from a set which might be concurrently modified.
     * Null values in the set are not supported.
     * The set should still be concurrent or synchronized via {@link java.util.Collections#synchronizedSet(Set)}.
     *
     * @param set
     * @param <T>
     * @return
     */
    public static <T> Optional<T> pollElementFromSet(Set<T> set) {
        while (!set.isEmpty()) {
            try {
                Iterator<T> iterator = set.iterator();
                if (!iterator.hasNext()) {
                    return Optional.empty();
                }
                return Optional.ofNullable(iterator.next());
            } catch (ConcurrentModificationException e) {
                //set was modified at teh same time- retry
            }
        }
        return Optional.empty();
    }

}
