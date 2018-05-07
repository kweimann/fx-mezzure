package kweimann.mezzure.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

public final class Collections {

    private static class _Window<A> implements Window<A> {
        private final A[] window;
        private int spotsLeft;
        private int start = 0;

        @SuppressWarnings("unchecked")
        private _Window(int size) {
            this.window = (A[]) new Object[size];
            this.spotsLeft = size;
        }

        @Override
        public A get(int i) {
            if (i < 0 || i >= size()) throw new NoSuchElementException();
            return window[(start + i) % window.length];
        }

        @Override
        public int size() {
            return window.length - spotsLeft;
        }

        private void add(A next) {
            if (full()) throw new RuntimeException();
            window[(start + (window.length - spotsLeft--)) % window.length] = next;
        }

        private A slide(A next) {
            A removed = window[start];
            window[start] = next;
            start = (start + 1) % window.length;
            return removed;
        }

        private boolean empty() {
            return spotsLeft == window.length;
        }

        private boolean full() {
            return spotsLeft == 0;
        }
    }

    private Collections() {}

    public static <A> Iterable<Window<A>> slideWindow(Iterable<A> iterable, int size) {
        return slideWindow(iterable, size, 0);
    }

    /**
     * slides a window over collection
     * @param iterable collection
     * @param size size of the window
     * @param padding number of null elements to prepend & append to the collection
     * @return window iterator
     */
    public static <A> Iterable<Window<A>> slideWindow(Iterable<A> iterable, int size, int padding) {
        if (padding >= size) throw new IllegalArgumentException("padding may not exceed size");
        return () -> new Iterator<Window<A>>() {
            Iterator<A> it = iterable.iterator();
            _Window<A> window = new _Window<>(size);
            int trailingNulls = padding;

            {
                // add size - 1 elements to the window
                int _size = 0;
                while (_size < padding) {
                    window.add(null);
                    _size++;
                }
                while (it.hasNext() && ++_size < size)
                    window.add(it.next());
                while (trailingNulls > 0 && ++_size < size) {
                    window.add(null);
                    trailingNulls--;
                }
            }

            @Override
            public boolean hasNext() {
                return it.hasNext() || trailingNulls > 0;
            }

            @Override
            public Window<A> next() {
                A next;

                if (it.hasNext()) next = it.next();
                else if (trailingNulls-- > 0) next = null;
                else throw new NoSuchElementException();

                if (window.full()) window.slide(next);
                else window.add(next);

                return window;
            }
        };
    }
}
