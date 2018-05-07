package kweimann.mezzure.controller;

import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import kweimann.mezzure.model.Element1D;
import kweimann.mezzure.model.Interval;
import kweimann.mezzure.model.Point;
import kweimann.mezzure.util.Collections;
import kweimann.mezzure.util.Window;
import kweimann.mezzure.view.DrawingContext;

import java.util.*;

public final class MezzureEventHandler implements EventHandler<MouseEvent> {
    private static final int intervalToleranceSpan = 10;
    private static final int pointToleranceSpan = 10;

    private final DrawingContext dc;

    // current drag event if user is dragging an element
    private DragEvent dragEvent;
    // last position while hovering over area not occupied by any element
    private Point lastHoverPosition;
    // sorted set of currently visible intervals
    private SortedSet<Interval> intervals = new TreeSet<>(Comparator.comparing(Interval::start));

    private ChangeListener listener;

    public MezzureEventHandler(DrawingContext dc) {
        this.dc = dc;
    }

    @Override
    public void handle(MouseEvent event) {
        // handle mouse event

        if (event.getEventType() == MouseEvent.MOUSE_PRESSED && dragEvent == null) {
            clearHoverPosition();
            Point dragStart = getPosition(event);
            if (dragStart != null) dragEvent = getDragEvent(dragStart);
        } else if (event.getEventType() == MouseEvent.MOUSE_RELEASED && dragEvent != null) {
            if (listener != null && dragEvent.result() != null && !dragEvent.result().equals(dragEvent.origin()))
                listener.onChange(dragEvent.origin(), dragEvent.result());
            dragEvent = null;
        } else if (event.getEventType() == MouseEvent.MOUSE_CLICKED && event.getClickCount() == 2) {
            Point position = getPosition(event);
            Interval interval;
            if (position != null && (interval = overlaps(position)) != null) {
                // remove selected interval
                clearBlankIntervals();
                dc.clear(interval);
                intervals.remove(interval);
                drawBlankIntervals();
                if (listener != null)
                    listener.onChange(interval, null);
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
                    dc.clear(prev);
                }
                intervals.add(next);
                dc.draw(next);
                drawBlankIntervals();
                if (listener != null && !next.equals(prev))
                    listener.onDrag(prev, next);
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
                        dc.setCursor(Cursor.H_RESIZE);
                    } else if (selection instanceof IntervalSelection) {
                        dc.setCursor(Cursor.MOVE);
                    } else throw new IllegalArgumentException("unknown selection");
                } else {
                    updateHoverPosition(position);
                    dc.setCursor(Cursor.DEFAULT);
                }
            } else {
                clearHoverPosition();
                dc.setCursor(Cursor.DEFAULT);
            }
        }
    }

    public void setListener(ChangeListener listener) {
        this.listener = listener;
    }

    public Iterable<Interval> getIntervals() {
        return intervals;
    }

    public void addInterval(Interval interval) {
        if (overlaps(interval) != null) throw new IllegalArgumentException("overlapping intervals illegal");
        clearBlankIntervals();
        intervals.add(interval);
        dc.draw(interval);
        drawBlankIntervals();
    }

    private void clearBlankIntervals() {
        for (Window<Interval> window : Collections.slideWindow(intervals, 2)) {
            Interval interval = new Interval(window.get(0).end(), window.get(1).start());
            dc.clearText(interval);
        }
    }

    private void drawBlankIntervals() {
        for (Window<Interval> window : Collections.slideWindow(intervals, 2)) {
            Interval interval = new Interval(window.get(0).end(), window.get(1).start());
            dc.addText(interval);
        }
    }

    private void clearHoverPosition() {
        updateHoverPosition(null);
    }

    private void updateHoverPosition(Point updated) {
        if (lastHoverPosition != null) {
            dc.clear(lastHoverPosition);
            lastHoverPosition = null;
        }
        if (updated != null) {
            lastHoverPosition = updated;
            dc.draw(lastHoverPosition);
        }
    }

    private Selection getSelection(Point point) {
        Selection selection = null;

        // choose the closest element (relative to the point) that overlaps the point
        for (Interval interval : intervals) {
            if (interval.overlaps(point) || point.distance(interval) <= intervalToleranceSpan) {
                Element1D selected;

                int distanceToStart = interval.start().distance(point);
                int distanceToEnd = interval.end().distance(point);

                // determine whether to select interval start/end points or the interval itself
                if (distanceToStart < distanceToEnd && distanceToStart <= pointToleranceSpan) {
                    selected = interval.start();
                } else if (distanceToStart > distanceToEnd && distanceToEnd <= pointToleranceSpan) {
                    selected = interval.end();
                } else {
                    selected = interval;
                }

                if (selection == null || selection.selected().distance(point) > selected.distance(point))
                    selection = selected instanceof Interval
                            ? new IntervalSelection((Interval) selected)
                            : new PointSelection((Point) selected, interval);
            }
        }

        return selection;
    }

    private Interval overlaps(Element1D element) {
        for (Interval interval : intervals)
            if (interval.overlaps(element)) return interval;
        return null;
    }

    private Point getPosition(MouseEvent event) {
        return getPosition(event, false);
    }

    private Point getPosition(MouseEvent event, boolean fitToBorder) {
        return dc.getPoint((int) event.getX(), (int) event.getY(), fitToBorder);
    }

    private DragEvent getDragEvent(Point dragStart) {
        Selection selection = getSelection(dragStart);

        if (selection != null) {
            if (selection instanceof IntervalSelection) {
                // move an existing interval
                IntervalSelection intervalSelection = (IntervalSelection) selection;

                SortedSet<Interval> remainingIntervals = new TreeSet<>(intervals);
                remainingIntervals.remove(intervalSelection.selected());

                return new IntervalMove(dragStart, intervalSelection.selected(), remainingIntervals, dc);
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
        // slide a window over all intervals to find the blank intervals in between
        for (Window<Interval> window : Collections.slideWindow(remainingIntervals, 2, 1)) {
            Interval dragSpace = getIntervalInBetween(window.get(0), window.get(1));
            if (dragSpace != null && dragSpace.overlaps(dragStart)) {
                return new IntervalResize(dragStart, dragSpace, origin);
            }
        }
        return null;
    }

    private Interval getIntervalInBetween(Interval prev, Interval next) {
        Point a = prev == null ? dc.getStart() : prev.end().move(1);
        Point b = next == null ? dc.getEnd() : next.start().move(-1);

        if (a.compareTo(b) >= 0) return null;

        return new Interval(a, b);
    }

    private interface DragEvent {
        Interval updated(Point dragEnd);
        Interval origin();
        Interval result();
    }

    private static class IntervalMove implements DragEvent {

        private final Point dragStart;
        private final Interval origin;
        private final Collection<Interval> remainingIntervals;
        private final DrawingContext gc;

        private Interval interval;

        IntervalMove(Point dragStart, Interval origin, Collection<Interval> remainingIntervals, DrawingContext gc) {
            this.dragStart = dragStart;
            this.origin = origin;
            this.remainingIntervals = remainingIntervals;
            this.gc = gc;
            this.interval = origin;
        }

        @Override
        public Interval updated(Point dragEnd) {
            int displacement = dragStart.displacement(dragEnd);

            displacement = displacement < 0
                    ? Math.max(displacement, origin.start().displacement(gc.getStart()))    // move left
                    : Math.min(displacement, origin.end().displacement(gc.getEnd()));       // move right

            interval = adjustIntervalOnOverlap(origin.move(displacement), dragEnd);
            return interval;
        }

        @Override
        public Interval origin() {
            return origin;
        }

        @Override
        public Interval result() {
            return interval;
        }

        private Interval adjustIntervalOnOverlap(Interval updated, Point dragEnd) {
            Interval overlapping = getClosestOverlappingInterval(updated);

            if (overlapping != null) {
                for (Window<Interval> window : Collections.slideWindow(remainingIntervals, 3, 1)) {
                    Interval interval = window.get(1);

                    if (interval.equals(overlapping)) {
                        // choose where to move `updated` based on the cursor's position relative to overlapping interval's center
                        if (interval.center() > dragEnd.center()) {
                            Interval prev = window.get(0);
                            Point lastLeftSpot = prev == null ? gc.getStart() : prev.end().move(1);

                            // check if `updated` fits in the space on interval's left
                            if (lastLeftSpot.displacement(interval.start()) > updated.length()) {
                                // move `updated` left
                                return new Interval(interval.start().move(-updated.length() - 1), interval.start().move(-1));
                            }
                        } else {
                            Interval next = window.get(2);
                            Point lastRightSpot = next == null ? gc.getEnd() : next.start().move(-1);

                            // check if `updated` fits in the space on interval's right
                            if (interval.end().displacement(lastRightSpot) > updated.length()) {
                                // move `updated` right
                                return new Interval(interval.end().move(updated.length() + 1), interval.end().move(1));
                            }
                        }

                        // no viable spot could be found so don't update interval at all
                        return this.interval;
                    }
                }
            }

            // `updated` does not overlap with any interval so allow the update
            return updated;
        }

        private Interval getClosestOverlappingInterval(Interval updated) {
            Interval overlapping = null;

            for (Interval interval : remainingIntervals) {
                if (interval.overlaps(updated)
                        && (overlapping == null || distance(overlapping.center(), updated.center())
                        > distance(interval.center(), updated.center()))) {
                    overlapping = interval;
                }
            }

            return overlapping;
        }

        private static int distance(int a, int b) {
            return Math.abs(b - a);
        }
    }

    private static class IntervalResize implements DragEvent {

        private final Point dragStart;
        private final Interval dragSpace;
        private final Interval origin;

        private Interval interval;

        IntervalResize(Point dragStart, Interval dragSpace, Interval origin) {
            this.dragStart = dragStart;
            this.dragSpace = dragSpace;
            this.origin = origin;
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
        public Interval origin() {
            return origin;
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
        Element1D selected();
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
