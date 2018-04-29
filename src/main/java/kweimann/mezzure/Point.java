package kweimann.mezzure;

import java.util.Comparator;

import static kweimann.mezzure.IntervalsUtil.absoluteX;
import static kweimann.mezzure.IntervalsUtil.point;

public final class Point implements Element, Comparable<Point> {

    private final int x;
    private final int i;

    Point(int x, int i) {
        this.x = x;
        this.i = i;
    }

    public int x() {
        return x;
    }

    public int i() {
        return i;
    }

    public int distance(Point that) {
        return absoluteX(that) - absoluteX(this);
    }

    @Override
    public Point move(int units) {
        return move(units, true);
    }

    public Point move(int units, boolean stickToStart) {
        if (units == 0) return this;
        else return point(absoluteX(this) + units, stickToStart);
    }

    @Override
    public boolean overlaps(Element element) {
        if (element instanceof Point) {
            return this.equals(element);
        } else if (element instanceof Interval) {
            return element.overlaps(this);
        } else throw new IllegalArgumentException("unsupported element");
    }

    @Override
    public int compareTo(Point that) {
        return Comparator
                .comparingDouble(IntervalsUtil::absoluteX)
                .compare(this, that);
    }

    @Override
    public boolean equals(Object that) {
        return that instanceof Point
                && absoluteX(this) == absoluteX((Point) that);
    }

    @Override
    public String toString() {
        return String.format("(%d, %d)", x, i);
    }
}
