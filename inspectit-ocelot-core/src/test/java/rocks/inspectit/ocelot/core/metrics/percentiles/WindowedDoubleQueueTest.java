package rocks.inspectit.ocelot.core.metrics.percentiles;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class WindowedDoubleQueueTest {

    @Nested
    class RoundUpToPowerOfTwo {

        @Test
        public void nonPowerOfTwoValues() {
            assertThat(WindowedDoubleQueue.roundUpToPowerOfTwo(0)).isEqualTo(0);
            assertThat(WindowedDoubleQueue.roundUpToPowerOfTwo(3)).isEqualTo(4);
            assertThat(WindowedDoubleQueue.roundUpToPowerOfTwo(5)).isEqualTo(8);
            assertThat(WindowedDoubleQueue.roundUpToPowerOfTwo(7)).isEqualTo(8);
            assertThat(WindowedDoubleQueue.roundUpToPowerOfTwo(22)).isEqualTo(32);
            assertThat(WindowedDoubleQueue.roundUpToPowerOfTwo(5000)).isEqualTo(8192);
        }

        @Test
        public void powerOfTwoValues() {
            assertThat(WindowedDoubleQueue.roundUpToPowerOfTwo(2)).isEqualTo(2);
            assertThat(WindowedDoubleQueue.roundUpToPowerOfTwo(4)).isEqualTo(4);
            assertThat(WindowedDoubleQueue.roundUpToPowerOfTwo(16)).isEqualTo(16);
            assertThat(WindowedDoubleQueue.roundUpToPowerOfTwo(256)).isEqualTo(256);
            assertThat(WindowedDoubleQueue.roundUpToPowerOfTwo(8192)).isEqualTo(8192);
        }

    }

    @Nested
    public class Insert {

        @Test
        void testAlignedGrowth() {
            WindowedDoubleQueue queue = new WindowedDoubleQueue(1);

            for (int i = 0; i <= WindowedDoubleQueue.MIN_CAPACITY; i++) {
                queue.insert(i * 100 + 1, 42);

            }

            double[] expectedResult = IntStream.range(0, WindowedDoubleQueue.MIN_CAPACITY + 1)
                    .mapToDouble(i -> i * 100 + 1)
                    .toArray();
            WindowedDoubleQueue.ValueCopy result = queue.copy(null);
            assertThat(result.getData()).isEqualTo(expectedResult);
            assertThat(result.getSize()).isEqualTo(expectedResult.length);

        }

        @Test
        void testUnalignedGrowth() {
            WindowedDoubleQueue queue = new WindowedDoubleQueue(1);

            queue.insert(-12345, 0);
            queue.insert(-12345, 0);
            queue.insert(-12345, 0);
            for (int i = 0; i <= WindowedDoubleQueue.MIN_CAPACITY; i++) {
                queue.insert(i * 100 + 1, 1);

            }

            double[] expectedResult = IntStream.range(0, WindowedDoubleQueue.MIN_CAPACITY + 1)
                    .mapToDouble(i -> i * 100 + 1)
                    .toArray();
            WindowedDoubleQueue.ValueCopy result = queue.copy(null);
            assertThat(result.getData()).isEqualTo(expectedResult);
            assertThat(result.getSize()).isEqualTo(expectedResult.length);

        }

        @Test
        void verifyStreamingMaintainsCapacity() {
            WindowedDoubleQueue queue = new WindowedDoubleQueue(WindowedDoubleQueue.MIN_CAPACITY);

            for (int i = 0; i < WindowedDoubleQueue.MIN_CAPACITY * 10; i++) {
                queue.insert(i, i);
            }

            assertThat(queue.capacity()).isEqualTo(WindowedDoubleQueue.MIN_CAPACITY);
            assertThat(queue.size()).isEqualTo(WindowedDoubleQueue.MIN_CAPACITY);
        }

        @Test
        void invalidTimestamp() {
            WindowedDoubleQueue queue = new WindowedDoubleQueue(42);

            queue.insert(1.0, 10);
            assertThatThrownBy(() -> queue.insert(2.0, 9)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class RemoveStaleValues {

        @Test
        void removeAllValues() {
            WindowedDoubleQueue queue = new WindowedDoubleQueue(1);

            for (int i = 0; i < WindowedDoubleQueue.MIN_CAPACITY * 100; i++) {
                queue.insert(i, 0);
            }
            queue.removeStaleValues(1);

            assertThat(queue.capacity()).isEqualTo(WindowedDoubleQueue.MIN_CAPACITY);
            assertThat(queue.size()).isEqualTo(0);
        }

        @Test
        void removeAllExceptOneValues() {
            WindowedDoubleQueue queue = new WindowedDoubleQueue(1);

            for (int i = 0; i < WindowedDoubleQueue.MIN_CAPACITY * 100; i++) {
                queue.insert(i, 0);
            }
            queue.insert(42, 1);
            queue.removeStaleValues(1);

            assertThat(queue.capacity()).isEqualTo(WindowedDoubleQueue.MIN_CAPACITY);
            assertThat(queue.copy(null).getData()).contains(42);
        }

        @Test
        void removeAllExceptFew() {
            WindowedDoubleQueue queue = new WindowedDoubleQueue(2);

            int keepCount = WindowedDoubleQueue.MIN_CAPACITY + 1;
            for (int i = 0; i < WindowedDoubleQueue.MIN_CAPACITY * 100; i++) {
                queue.insert(-9999999, 0);
            }
            for (int i = 0; i < keepCount; i++) {
                queue.insert(42 + i, 1);
            }
            queue.removeStaleValues(2);

            double[] expectedResult = IntStream.range(0, keepCount).mapToDouble(i -> 42 + i).toArray();
            assertThat(queue.capacity()).isEqualTo(WindowedDoubleQueue.MIN_CAPACITY * 4);
            assertThat(queue.copy(null).getData()).isEqualTo(expectedResult);
        }

        @Test
        void removeAllExceptFewWithOverflow() {
            WindowedDoubleQueue queue = new WindowedDoubleQueue(WindowedDoubleQueue.MIN_CAPACITY * 2);

            int keepCount = WindowedDoubleQueue.MIN_CAPACITY + 1;
            int time = 0;

            for (int i = 0; i < WindowedDoubleQueue.MIN_CAPACITY * 3; i++) {
                queue.insert(-9999999, time);
                time++;
            }
            for (int i = 0; i < keepCount; i++) {
                queue.insert(42 + i, time + 1);
            }
            queue.removeStaleValues(time + WindowedDoubleQueue.MIN_CAPACITY * 2);

            double[] expectedResult = IntStream.range(0, keepCount).mapToDouble(i -> 42 + i).toArray();
            assertThat(queue.capacity()).isEqualTo(WindowedDoubleQueue.MIN_CAPACITY * 4);
            assertThat(queue.copy(null).getData()).isEqualTo(expectedResult);
        }

    }

    @Nested
    class Copy {

        @Test
        void copyEmptyIntoNullBuffer() {
            WindowedDoubleQueue queue = new WindowedDoubleQueue(1);

            WindowedDoubleQueue.ValueCopy copy = queue.copy();

            assertThat(copy.getData()).isEmpty();
            assertThat(copy.getSize()).isEqualTo(0);
        }

        @Test
        void copyEmptyIntoExistingBuffer() {
            WindowedDoubleQueue queue = new WindowedDoubleQueue(1);
            double[] buffer = new double[42];

            WindowedDoubleQueue.ValueCopy copy = queue.copy(buffer);

            assertThat(copy.getData()).isSameAs(buffer);
            assertThat(copy.getSize()).isEqualTo(0);
        }

        @Test
        void copyValuesIntoNullBuffer() {
            WindowedDoubleQueue queue = new WindowedDoubleQueue(1);
            for (int i = 0; i < 100; i++) {
                queue.insert(i, 0);
            }

            WindowedDoubleQueue.ValueCopy copy = queue.copy();

            double[] expectedResult = IntStream.range(0, 100).mapToDouble(i -> i).toArray();
            assertThat(copy.getData()).isEqualTo(expectedResult);
            assertThat(copy.getSize()).isEqualTo(100);
        }

        @Test
        void copyValuesIntoExistingBuffer() {
            WindowedDoubleQueue queue = new WindowedDoubleQueue(1);
            double[] buffer = new double[100];
            for (int i = 0; i < 100; i++) {
                queue.insert(i, 0);
            }

            WindowedDoubleQueue.ValueCopy copy = queue.copy(buffer);

            double[] expectedResult = IntStream.range(0, 100).mapToDouble(i -> i).toArray();
            assertThat(copy.getData()).isEqualTo(expectedResult);
            assertThat(copy.getData()).isSameAs(buffer);
            assertThat(copy.getSize()).isEqualTo(100);
        }

        @Test
        void copyValuesIntoTooSmallBuffer() {
            WindowedDoubleQueue queue = new WindowedDoubleQueue(1);
            for (int i = 0; i < 100; i++) {
                queue.insert(i, 0);
            }

            WindowedDoubleQueue.ValueCopy copy = queue.copy(new double[99]);

            double[] expectedResult = IntStream.range(0, 100).mapToDouble(i -> i).toArray();
            assertThat(copy.getData()).isEqualTo(expectedResult);
            assertThat(copy.getSize()).isEqualTo(100);
        }
    }
}
