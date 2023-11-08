package rocks.inspectit.ocelot.core.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RingBuffer<T> {

    private final AtomicInteger currentIndex = new AtomicInteger(0);

    private final AtomicInteger bufferSize;
    private final List<T> buffer;

    public RingBuffer (int bufferSize) {
        this.bufferSize = new AtomicInteger(bufferSize);
        buffer = new ArrayList<>(bufferSize);
    }

    public void put(T element) {
        int index = currentIndex.getAndIncrement();
        if (index >= bufferSize.get()) {
            currentIndex.set(0);
            index = 0;
        }
        buffer.add(index, element);
    }

    public T get(int index) {
        return buffer.get(index % buffer.size());
    }

    public List<T> asList() {
        if (buffer.isEmpty()) {
            return Collections.emptyList();
        }
        int oldestIndex = currentIndex.get() - 1;
        List<T> elementList = new ArrayList<>();
        for (int i = oldestIndex; i < oldestIndex + this.buffer.size(); i++) {
            elementList.add(get(i));
        }
        return elementList;
    }

}
