package kweimann.mezzure;

public final class Interval implements Element {

    private final Point start;
    private final Point end;

    public Interval(Point a, Point b) {
        int compare = a.compareTo(b);
        if (compare < 0) {
            this.start = a;
            this.end = b;
        } else if (compare > 0) {
            this.start = b;
            this.end = a;
        } else throw new IllegalArgumentException("points overlap");
    }

    public int length() {
        return start.distance(end);
    }

    public Point center() {
        return start.move(length() / 2);
    }

    public Interval resize(int difference) {
        return resize(-difference, difference);
    }

    public Interval resize(int startDifference, int endDifference) {
        Point start = this.start.move(startDifference);
        Point end = this.end.move(endDifference);
        if (start.compareTo(end) >= 0) throw new IllegalArgumentException("bad resize parameters");
        return new Interval(start, end);
    }

    @Override
    public Interval move(int units) {
        return new Interval(start.move(units, true), end.move(units, false));
    }

    @Override
    public boolean overlaps(Element element) {
        if (element instanceof Point) {
            return contains((Point) element);
        } else if (element instanceof Interval) {
            Interval that = (Interval) element;
            return this.contains(that.start)
                    || this.contains(that.end)
                    || that.contains(this.start)
                    || that.contains(this.end);
        } else throw new IllegalArgumentException("unsupported element");
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

    public Point start() {
        return start;
    }

    public Point end() {
        return end;
    }

    public int startX() {
        return start.x();
    }

    public int startI() {
        return start.i();
    }

    public int endX() {
        return end.x();
    }

    public int endI() {
        return end.i();
    }

    private boolean contains(Point p) {
        return start.compareTo(p) <= 0 && end.compareTo(p) >= 0;
    }
}
