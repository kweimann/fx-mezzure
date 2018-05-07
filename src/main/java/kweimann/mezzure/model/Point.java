package kweimann.mezzure.model;

import java.util.Comparator;

public final class Point implements Element1D, Comparable<Point> {

    private final int x;

    public Point(int x) {
        this.x = x;
    }

    @Override
    public int center() {
        return x;
    }

    @Override
    public int length() {
        return 0;
    }

    @Override
    public Point move(int n) {
        return new Point(x + n);
    }

    @Override
    public int compareTo(Point that) {
        return Comparator
                .comparingInt(Point::center)
                .compare(this, that);
    }

    @Override
    public boolean equals(Object that) {
        return that instanceof Point && this.x == ((Point) that).x;
    }

    @Override
    public String toString() {
        return String.format("(%d)", x);
    }

    @Override
    public int hashCode() {
        return x;
    }
}
