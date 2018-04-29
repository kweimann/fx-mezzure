package kweimann.mezzure;

public final class IntervalsUtil {

    public static final int width                       = 1000;
    public static final int height                      = 3000;

    public static final int sectionStart                = 100;
    public static final int sectionEnd                  = 900;
    public static final int sectionHeight               = 100;
    public static final int offsetY                     = 50;
    public static final int distanceBetweenSections     = 100;

    public static final Point start                     = new Point(0, 0);
    public static final Point end                       = new Point(sectionWidth(), sectionCount() - 1);

    private IntervalsUtil() {}

    // Y coordinate on canvas
    public static int Y(Point p) {
        return Y(p.i());
    }

    // Y coordinate on canvas
    public static int Y(int i) {
        return offsetY + i * (sectionHeight + distanceBetweenSections);
    }

    // X coordinate on canvas
    public static int X(Point p) {
        return X(p.x());
    }

    // X coordinate on canvas
    public static int X(int x) {
        return sectionStart + x;
    }

    // get point based on X, Y coordinates on canvas
    // if fitToBorder is true find the nearest point if X, Y coordinates are invalid
    // else if X or Y is not valid return null
    public static Point point(int X, int Y, boolean fitToBorder) {
        int x = translateX(X, fitToBorder);
        int i = translateI(Y, fitToBorder);
        return x != -1 && i != -1 ? new Point(x, i) : null;
    }

    public static Point point(int X, int Y) {
        return point(X, Y, false);
    }

    // translate X coordinate on canvas to x coordinate of a point
    // if fitToBorder is true find the nearest x if X is not valid
    // else if X is not valid return -1
    public static int translateX(int X, boolean fitToBorder) {
        return fitToBorder
                ? Math.min(Math.max(0, X - sectionStart), sectionWidth())
                : X >= sectionStart && X <= sectionEnd
                    ? X - sectionStart
                    : -1;
    }

    public static int translateX(int X) {
        return translateX(X, false);
    }

    // translate Y coordinate on canvas to i coordinate of a point
    // if fitToBorder is true find the nearest i if Y is not valid
    // else if Y is not valid return -1
    public static int translateI(int Y, boolean fitToBorder) {
        return fitToBorder
                // find nearest i
                ? Y > offsetY + distanceBetweenSections / 2
                    ? Y < height
                        ? sectionCount(Y - distanceBetweenSections / 2)
                        : sectionCount() - 1
                    : 0
                // find matching i
                : Y > offsetY
                && Y < height
                && (Y - offsetY - 1) % (sectionHeight + distanceBetweenSections) <= sectionHeight - 1
                    ? ceilDiv(Y - offsetY, sectionHeight + distanceBetweenSections) - 1
                    : -1;
    }

    public static int translateI(int Y) {
        return translateI(Y, false);
    }

    public static int sectionCount() {
        return sectionCount(height);
    }

    public static int sectionWidth() {
        return sectionEnd - sectionStart;
    }

    public static int absoluteX(Point p) {
        return p.x() + p.i() * sectionWidth();
    }

    // get point based on absolute x within sections
    // if stickToStart is true Point(<sectionWidth>, <i>) will be instead treated as Point(0, <i + 1>)
    // if stickToStart is false Point(0, <i>) will be instead treated as Point(<sectionWidth>, <i - 1>)
    public static Point point(int absoluteX, boolean stickToStart) {
        if (absoluteX == 0) return start;
        else return stickToStart
                ? new Point(absoluteX % sectionWidth(), absoluteX / sectionWidth())
                : new Point(((absoluteX - 1) % sectionWidth()) + 1, (absoluteX - 1) / sectionWidth());
    }

    public static Point point(int absoluteX) {
        return point(absoluteX, true);
    }

    // number of sections based on Y coordinate on canvas
    private static int sectionCount(int Y) {
        return Y > offsetY
                ? (Y - offsetY - 1) % (sectionHeight + distanceBetweenSections) >= sectionHeight - 1
                    ? ceilDiv(Y - offsetY, sectionHeight + distanceBetweenSections)
                    : (Y - offsetY) / (sectionHeight + distanceBetweenSections)
                : 0;
    }

    private static int ceilDiv(int x, int n) {
        return (x + n - 1) / n;
    }
}
