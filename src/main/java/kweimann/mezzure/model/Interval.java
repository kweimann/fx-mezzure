package kweimann.mezzure.model;

public final class Interval implements Element1D {

    private final Point start;
    private final Point end;

    public Interval(Point a, Point b) {
        if (a.center() < b.center()) {
            this.start = a;
            this.end = b;
        } else if (a.center() > b.center()) {
            this.start = b;
            this.end = a;
        } else throw new IllegalArgumentException("points overlap");
    }

    public Point start() {
        return start;
    }

    public Point end() {
        return end;
    }

    public Interval resize(int difference) {
        return resize(-difference, difference);
    }

    public Interval resize(int startDifference, int endDifference) {
        Point start = this.start.move(startDifference);
        Point end = this.end.move(endDifference);
        if (start.center() >= end.center())
            throw new IllegalArgumentException("bad resize parameters");
        return new Interval(start, end);
    }

    @Override
    public int center() {
        return start.center() + length() / 2;
    }

    @Override
    public int length() {
        return end.center() - start.center();
    }

    @Override
    public Interval move(int n) {
        return new Interval(start.move(n), end.move(n));
    }

    @Override
    public boolean equals(Object that) {
        return that instanceof Interval
                && this.start.equals(((Interval) that).start)
                && this.end.equals(((Interval) that).end);
    }

    @Override
    public String toString() {
        return String.format("[%s, %s]", start, end);
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 37 * result + start.hashCode();
        result = 37 * result + end.hashCode();
        return result;
    }
}
