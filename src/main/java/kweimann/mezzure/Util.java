package kweimann.mezzure;

import java.util.*;

public final class Util {

    private Util() {}

    public static int distance(Element e1, Element e2) {
        if (e1 instanceof Point && e2 instanceof Point) {
            return Math.abs(((Point) e1).distance((Point) e2));
        } else if (e1 instanceof Point && e2 instanceof Interval) {
            Interval interval = (Interval) e2;
            return Math.min(distance(e1, interval.start()), distance(e1, interval.end()));
        } else if (e1 instanceof Interval && e2 instanceof Point) {
            return distance(e2, e1);
        } else if (e1 instanceof Interval && e2 instanceof Interval) {
            Interval interval = (Interval) e1;
            return Math.min(distance(interval.start(), e2), distance(interval.end(), e2));
        } else throw new IllegalArgumentException("element not supported");
    }

    public static <T> Iterable<List<T>> slideWindow(Collection<T> coll, int size) {
        return slideWindow(coll, size, 0);
    }

    public static <T> Iterable<List<T>> slideWindow(Collection<T> coll, int size, int padding) {
        assert padding < size;
        return () -> new Iterator<List<T>>() {
            Iterator<T> it = coll.iterator();
            LinkedList<T> window = new LinkedList<>();
            int trailingNulls = padding;

            {
                int _padding = padding;
                while (_padding-- > 0) {
                    window.addLast(null);
                }
                int _size = size - padding;
                if (coll.size() >= _size)
                    while (--_size > 0) {
                        window.addLast(it.next());
                    }
                else {
                    _size = coll.size();
                    while (_size-- > 0) {
                        window.addLast(it.next());
                    }
                    _size = size - padding - coll.size();
                    while (--_size > 0) {
                        window.addLast(null);
                        trailingNulls--;
                    }
                }
            }

            @Override
            public boolean hasNext() {
                return it.hasNext() || trailingNulls > 0;
            }

            @Override
            public List<T> next() {
                T next;

                if (it.hasNext()) {
                    next = it.next();
                } else if (trailingNulls-- > 0) {
                    next = null;
                } else throw new NoSuchElementException();

                if (window.size() == size)
                    window.removeFirst();
                window.addLast(next);

                return window;
            }
        };
    }
}
