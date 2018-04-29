package kweimann.mezzure;

import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;

import java.util.*;

import static kweimann.mezzure.IntervalsUtil.*;

public final class IntervalsEventHandler implements EventHandler<MouseEvent> {
    private static final int intervalToleranceSpan = 10;
    private static final int pointToleranceSpan = 10;

    private final Painter painter;

    // current drag event if user is dragging an element
    private DragEvent dragEvent;
    // last position while hovering over area not occupied by any element
    private Point lastHoverPosition;
    // sorted set of currently visible intervals
    private SortedSet<Interval> intervals = new TreeSet<>(Comparator.comparing(Interval::start));

    public IntervalsEventHandler(Painter painter) {
        this.painter = painter;
    }

    @Override
    public void handle(MouseEvent event) {
        // handle mouse event

        if (event.getEventType() == MouseEvent.MOUSE_PRESSED && dragEvent == null) {
            clearHoverPosition();
            Point dragStart = getPosition(event);
            if (dragStart != null) dragEvent = getDragEvent(dragStart);
        } else if (event.getEventType() == MouseEvent.MOUSE_RELEASED && dragEvent != null) {
            dragEvent = null;
        } else if (event.getEventType() == MouseEvent.MOUSE_CLICKED && event.getClickCount() == 2) {
            Point position = getPosition(event);
            Interval interval;
            if (position != null && (interval = overlaps(position)) != null) {
                clearBlankIntervals();
                painter.clear(interval);
                intervals.remove(interval);
                paintBlankIntervals();
            }
        } else if (dragEvent != null) {
            // when fitToBorder is true a point is always available
            Point dragEnd = getPosition(event, true);
            Interval prev = dragEvent.result();
            Interval next = dragEvent.updated(dragEnd);
            // if next == null then prev == null
            if (next != null) { // creating new interval may produce null if creation has been registered
                                // but the interval has not been valid yet
                // before each update clear blank intervals then after the update repaint them
                clearBlankIntervals();
                if (prev != null) {
                    // during the initial phase of creating new interval there is no previous interval yet
                    intervals.remove(prev);
                    painter.clear(prev);
                }
                intervals.add(next);
                painter.paint(next);
                paintBlankIntervals();
            }
        }

        // handle cursor updates

        if (dragEvent == null) {
            Point position = getPosition(event);
            if (position != null) {
                Selection selection = getSelection(position);
                if (selection != null) {
                    clearHoverPosition();
                    if (selection instanceof PointSelection) {
                        painter.setCursor(Cursor.H_RESIZE);
                    } else if (selection instanceof IntervalSelection) {
                        painter.setCursor(Cursor.MOVE);
                    } else throw new IllegalArgumentException("unknown selection");
                } else {
                    updateHoverPosition(position);
                    painter.setCursor(Cursor.DEFAULT);
                }
            } else {
                clearHoverPosition();
                painter.setCursor(Cursor.DEFAULT);
            }
        }
    }

    private void clearBlankIntervals() {
        for (List<Interval> window : Util.slideWindow(intervals, 2)) {
            Interval interval = new Interval(window.get(0).end(), window.get(1).start());
            painter.clearText(interval.start());
        }
    }

    private void paintBlankIntervals() {
        for (List<Interval> window : Util.slideWindow(intervals, 2)) {
            Interval interval = new Interval(window.get(0).end(), window.get(1).start());
            painter.paintText(Integer.toString(interval.length()), interval.start());
        }
    }

    private void clearHoverPosition() {
        updateHoverPosition(null);
    }

    private void updateHoverPosition(Point updated) {
        if (lastHoverPosition != null) {
            painter.clear(lastHoverPosition);
            lastHoverPosition = null;
        }
        if (updated != null) {
            lastHoverPosition = updated;
            painter.paint(lastHoverPosition);
        }
    }

    private Selection getSelection(Point point) {
        Selection selection = null;

        for (Interval interval : intervals) {
            if (interval.overlaps(point) || Util.distance(point, interval) <= intervalToleranceSpan) {
                Element selected;

                int distanceToStart = Util.distance(interval.start(), point);
                int distanceToEnd = Util.distance(interval.end(), point);

                if (distanceToStart < distanceToEnd && distanceToStart <= pointToleranceSpan) {
                    selected = interval.start();
                } else if (distanceToStart > distanceToEnd && distanceToEnd <= pointToleranceSpan) {
                    selected = interval.end();
                } else {
                    selected = interval;
                }

                if (selection == null || Util.distance(selection.selected(), point) > Util.distance(selected, point))
                    selection = selected instanceof Interval
                            ? new IntervalSelection((Interval) selected)
                            : new PointSelection((Point) selected, interval);
            }
        }

        return selection;
    }

    private Interval overlaps(Point point) {
        for (Interval interval : intervals)
            if (interval.overlaps(point)) return interval;
        return null;
    }

    private Point getPosition(MouseEvent event) {
        return getPosition(event, false);
    }

    private Point getPosition(MouseEvent event, boolean fitToBorder) {
        return point((int) event.getX(), (int) event.getY(), fitToBorder);
    }

    private DragEvent getDragEvent(Point dragStart) {
        Selection selection = getSelection(dragStart);

        if (selection != null) {
            if (selection instanceof IntervalSelection) {
                // move an existing interval
                IntervalSelection intervalSelection = (IntervalSelection) selection;

                SortedSet<Interval> remainingIntervals = new TreeSet<>(intervals);
                remainingIntervals.remove(intervalSelection.selected());

                return new IntervalMove(dragStart, intervalSelection.selected(), remainingIntervals);
            } else if (selection instanceof PointSelection) {
                // resize an existing interval
                PointSelection pointSelection = (PointSelection) selection;

                SortedSet<Interval> remainingIntervals = new TreeSet<>(intervals);
                remainingIntervals.remove(pointSelection.parent());

                return getResizeEvent(pointSelection.notSelected(), pointSelection.parent(), remainingIntervals);
            } else throw new IllegalArgumentException("unknown selection");
        } else {
            // create a new interval
            return getResizeEvent(dragStart, null, intervals);
        }
    }

    private IntervalResize getResizeEvent(Point dragStart, Interval origin, Collection<Interval> remainingIntervals) {
        for (List<Interval> window : Util.slideWindow(remainingIntervals, 2, 1)) {
            Interval dragSpace = getIntervalInBetween(window.get(0), window.get(1));
            if (dragSpace != null && dragSpace.overlaps(dragStart)) {
                return new IntervalResize(dragStart, dragSpace, origin);
            }
        }
        return null;
    }

    private Interval getIntervalInBetween(Interval prev, Interval next) {
        Point a = prev == null ? start : prev.end().equals(end) ? end : prev.end().move(1);
        Point b = next == null ? end : next.start().equals(start) ? start : next.start().move(-1);

        if (a.equals(b)) return null;

        assert a.compareTo(b) < 0;

        return new Interval(a, b);
    }

    private interface DragEvent {
        Interval updated(Point dragEnd);
        Interval result();
    }

    private static class IntervalMove implements DragEvent {

        private final Point dragStart;
        private final Interval origin;
        private final Collection<Interval> remainingIntervals;

        private Interval interval;

        IntervalMove(Point dragStart, Interval origin, Collection<Interval> remainingIntervals) {
            this.dragStart = dragStart;
            this.origin = origin;
            this.remainingIntervals = remainingIntervals;
            this.interval = origin;
        }

        @Override
        public Interval updated(Point dragEnd) {
            int distance = dragStart.distance(dragEnd);

            distance = distance < 0
                    ? Math.max(distance, origin.start().distance(start))    // move left
                    : Math.min(distance, origin.end().distance(end));       // move right

            interval = adjustIntervalOnOverlap(origin.move(distance), dragEnd);
            return interval;
        }

        @Override
        public Interval result() {
            return interval;
        }

        private Interval adjustIntervalOnOverlap(Interval updated, Point dragEnd) {
            for (List<Interval> window : Util.slideWindow(remainingIntervals, 3, 1)) {
                Interval interval = window.get(1);

                if (interval.overlaps(updated)) {
                    // choose where to move `updated` based on the cursor's position relative to overlapping interval's center
                    if (interval.center().compareTo(dragEnd) > 0) {
                        Interval prev = window.get(0);
                        Point lastLeftSpot = prev == null ? start : prev.end().move(1);

                        // check if `updated` fits in the space on interval's left
                        if (lastLeftSpot.distance(interval.start()) > updated.length()) {
                            // move `updated` left
                            return new Interval(interval.start().move(-updated.length() - 1, true),
                                    interval.start().move(-1, false));
                        }
                    } else {
                        Interval next = window.get(2);
                        Point lastRightSpot = next == null ? end : next.start().move(-1);

                        // check if `updated` fits in the space on interval's right
                        if (interval.end().distance(lastRightSpot) > updated.length()) {
                            // move `updated` right
                            return new Interval(interval.end().move(updated.length() + 1, false),
                                    interval.end().move(1, true));
                        }
                    }

                    // no viable spot could be found so don't update interval at all
                    return this.interval;
                }
            }

            // `updated` does not overlap with any interval so allow the update
            return updated;
        }
    }

    private static class IntervalResize implements DragEvent {

        private final Point dragStart;
        private final Interval dragSpace;

        private Interval interval;

        IntervalResize(Point dragStart, Interval dragSpace, Interval origin) {
            this.dragStart = dragStart;
            this.dragSpace = dragSpace;
            this.interval = origin;
        }

        @Override
        public Interval updated(Point dragEnd) {
            dragEnd = fitToDragSpace(dragEnd);
            if (!dragStart.equals(dragEnd))
                interval = new Interval(dragStart, dragEnd);
            return interval;
        }

        @Override
        public Interval result() {
            return interval;
        }

        private Point fitToDragSpace(Point position) {
            if (position.compareTo(dragSpace.start()) < 0) return dragSpace.start();
            else if (position.compareTo(dragSpace.end()) > 0) return dragSpace.end();
            else return position;
        }
    }

    private interface Selection {
        Element selected();
    }

    private static class IntervalSelection implements Selection {

        private final Interval selected;

        IntervalSelection(Interval selected) {
            this.selected = selected;
        }

        @Override
        public Interval selected() {
            return selected;
        }
    }

    private static class PointSelection implements Selection {
        private final Point selected;
        private final Interval parent;

        PointSelection(Point selected, Interval parent) {
            this.selected = selected;
            this.parent = parent;
        }

        @Override
        public Point selected() {
            return selected;
        }

        Interval parent() {
            return parent;
        }

        Point notSelected() {
            if (parent.start().equals(selected)) return parent.end();
            else if (parent.end().equals(selected)) return parent.start();
            else throw new NoSuchElementException("bad selection");
        }
    }
}
