package rocks.inspectit.ocelot.core.metrics.percentiles;

import com.google.common.annotations.VisibleForTesting;

/**
 * A circular, array based FIFO-queue for remembering measurement values in a sliding window over time.
 * <p>
 * A queue covers a fixed time range (e.g. 15 seconds).
 * When new points are inserted into the queue, all data which older than this timerange will be evicted.
 * It is expected that points are inserted in the order of time, each newer values are inserted after older values.
 * <p>
 * Similar to an ArrayList this queue is not bounded in size. It grows as needed.
 * However in contrast to an ArrayList, this queue also deallocates memory when less than 25% of it is occupied.
 * <p>
 * This data structure is not thread safe!
 */
public class WindowedDoubleQueue {

    @VisibleForTesting
    static final int MIN_CAPACITY = 16;

    /**
     * The factor used when increasing or decreasing the capacity.
     */
    private static final int CAPACITY_SCALING_FACTOR = 2;

    /**
     * Holds the values inserted into the queue.
     * values[startIndex] is the older value, values[(startIndex+size)%values.length] is the most recent one.
     */
    private double[] values;

    /**
     * Holds the timestamps for the points stored in {@link #values}.
     * E.g. the timestamp of value[42] is found at timeStamps[42];
     */
    private long[] timeStamps;

    /**
     * The index of the first (= the oldest) element in the queue within {@link #values}.
     */
    private int startIndex;

    /**
     * The number of elements stored in the queue.
     */
    private int size;

    /**
     * The size of the time window covered by this queue.
     */
    private long timeRange;

    /**
     * Creates a new queue, covering the given amount of time.
     *
     * @param timeRange the time after which old values will be evicted from the queue.
     *                  The unit must be the same as for timestamps given to {@link #insert(double, long)}
     *                  and {@link #removeStaleValues(long)}
     */
    public WindowedDoubleQueue(long timeRange) {
        values = new double[MIN_CAPACITY];
        timeStamps = new long[MIN_CAPACITY];
        this.timeRange = timeRange;
    }

    /**
     * Inserts a new point into the queue.
     * This call does not remove stale data, you should call removeStaleValues() before.
     * <p>
     * The queue expects that all inserts happen ordered in time!
     * You should never insert data which is older than the latest element in the queue.
     * <p>
     * This method has an amortized O(1) runtime, with a worst case of O(n).
     * <p>
     * In addition, this method is guaranteed to not alter the queue in case it throws an exception.
     *
     * @param value     the value of the new observation to insert
     * @param timeStamp the timestamp of the point to insert
     */
    public void insert(double value, long timeStamp) {
        if (size > 0 && timeStamps[normalizeIndex(startIndex + size - 1)] > timeStamp) {
            throw new IllegalArgumentException("The provided timestamp is older than the most recent timestamp present in the queue");
        }
        if (size == capacity()) {
            increaseCapacity();
        }
        int insertIdx = normalizeIndex(startIndex + size);
        values[insertIdx] = value;
        timeStamps[insertIdx] = timeStamp;
        size++;
    }

    /**
     * Evicts all points from the queue which have fallen out of the time window.
     *
     * @param nowTimeStamp the time stamp which represents the current point in time.
     *                     E.g. if the timeRange for this queue is 10s this method is called t=72s, all points with a timestamp older than 62s will be erased.
     *
     * @return the number of points which have been removed from this queue
     */
    public int removeStaleValues(long nowTimeStamp) {
        long timeLimit = nowTimeStamp - timeRange;
        int removedCount = 0;
        while (size > 0) {
            if (timeStamps[startIndex] <= timeLimit) {
                startIndex = normalizeIndex(startIndex + 1);
                size--;
                removedCount++;
            } else {
                break;
            }
        }
        trimToSize();
        return removedCount;
    }

    /**
     * @return the number of points currently contained in this queue
     */
    public int size() {
        return size;
    }

    /**
     * Copies the values of all points in this queue into a newly allocated array.
     *
     * @return the newly allocated array populated with the contents of this queue
     */
    public double[] copy() {
        double[] output = new double[size];
        copy(output);
        return output;
    }

    /**
     * Copies the values of all points in this queue into a provided array.
     * The provided array must have at least the size of this queue!
     */
    public void copy(double[] resultBuffer) {
        if (resultBuffer.length < size) {
            throw new IllegalArgumentException("The provided array is too small!");
        }
        copyValues(resultBuffer);
    }

    @VisibleForTesting
    int capacity() {
        return values.length;
    }

    private int normalizeIndex(int idx) {
        // this is the same as idx % values.length, because capacity is always a power of 2
        return idx & (capacity() - 1);
    }

    private void increaseCapacity() {
        resize(capacity() * CAPACITY_SCALING_FACTOR);
    }

    private void trimToSize() {
        int desiredCapacity = Math.max(MIN_CAPACITY, roundUpToPowerOfTwo(size * CAPACITY_SCALING_FACTOR));
        if (desiredCapacity < capacity()) {
            resize(desiredCapacity);
        }
    }

    private void resize(int newCapacity) {
        double[] newValues = new double[newCapacity];
        long[] newTimeStamps = new long[newCapacity];
        copyValues(newValues);
        copyTimestamps(newTimeStamps);
        values = newValues;
        timeStamps = newTimeStamps;
        startIndex = 0;
    }

    private void copyValues(double[] destination) {
        int capacity = capacity();
        if ((startIndex + size) <= capacity) {
            System.arraycopy(values, startIndex, destination, 0, size);
        } else { //our circular buffer overlaps the end of the array
            int count = capacity - startIndex;
            System.arraycopy(values, startIndex, destination, 0, count);
            System.arraycopy(values, 0, destination, count, size - count);
        }
    }

    private void copyTimestamps(long[] destination) {
        int capacity = capacity();
        if ((startIndex + size) <= capacity) {
            System.arraycopy(timeStamps, startIndex, destination, 0, size);
        } else { //our circular buffer overlaps the end of the array
            int count = capacity - startIndex;
            System.arraycopy(timeStamps, startIndex, destination, 0, count);
            System.arraycopy(timeStamps, 0, destination, count, size - count);
        }
    }

    @VisibleForTesting
    static int roundUpToPowerOfTwo(int value) {
        int highestOneBit = Integer.highestOneBit(value);
        if (value == highestOneBit) {
            return value;
        }
        return highestOneBit * 2;
    }

}
