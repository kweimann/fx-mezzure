package kweimann.mezzure.view;

import javafx.scene.Cursor;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.Text;
import kweimann.mezzure.model.Element1D;
import kweimann.mezzure.model.Interval;
import kweimann.mezzure.model.Point;

import java.util.HashMap;
import java.util.Map;

public class MezzureDrawingContext implements DrawingContext {

    private final Mezzure ctx;
    private final MezzureConfiguration cfg;
    private final GraphicsContext gc;

    private final Point start;
    private final Point end;

    private final Map<Element1D, Text> labels = new HashMap<>();

    MezzureDrawingContext(Mezzure ctx) {
        this.ctx = ctx;
        this.cfg = ctx.configuration;
        this.gc = ctx.getGraphicsContext2D();
        this.start = new Point(0);
        this.end = new Point(cfg.sectionWidth() * sectionCount());
    }

    @Override
    public void addText(Element1D element) {
        String content;
        if (ctx.descriptor != null && (content = ctx.descriptor.apply(element)) != null) {
            if (element instanceof Point) {
                addText(content, (Point) element, true, element);
            } else if (element instanceof Interval) {
                addText(content, new Point(element.center()), true, element);
            } else throw new IllegalArgumentException("element not supported");
        }
    }

    @Override
    public void clearText(Element1D element) {
        if (ctx.descriptor != null) {
            Text removed = labels.remove(element);
            if (removed != null)
                ctx.root.getChildren().remove(removed);
        }
    }

    @Override
    public void draw(Element1D element) {
        if (element instanceof Point) {
            drawPoint((Point) element, true);
        } else if (element instanceof Interval) {
            drawInterval((Interval) element);
        } else throw new IllegalArgumentException("element not supported");
    }

    @Override
    public void clear(Element1D element) {
        if (element instanceof Point) {
            clearPoint((Point) element);
        } else if (element instanceof Interval) {
            clearInterval((Interval) element);
        } else throw new IllegalArgumentException("element not supported");
    }

    @Override
    public void setCursor(Cursor cursor) {
        ctx.setCursor(cursor);
    }

    @Override
    public Point getPoint(int canvasX, int canvasY, boolean fitToSection) {
        int sectionX, sectionY;

        if (fitToSection) {
            sectionX = Math.min(Math.max(0, canvasX - cfg.sectionStart()), cfg.sectionWidth());
            sectionY = canvasY > cfg.offsetY() + cfg.distanceBetweenSections() / 2
                    ? canvasY < cfg.height()
                        ? sectionCount(canvasY - cfg.distanceBetweenSections() / 2)
                        : sectionCount() - 1
                    : 0;
        } else {
            if (canvasX >= cfg.sectionStart()
                    && canvasX <= cfg.sectionStart() + cfg.sectionWidth()
                    && canvasY > cfg.offsetY() && canvasY < cfg.height()
                    && (canvasY - cfg.offsetY() - 1) % (cfg.sectionHeight() + cfg.distanceBetweenSections()) <= cfg.sectionHeight() - 1) {
                sectionX = canvasX - cfg.sectionStart();
                sectionY = ceilDiv(canvasY - cfg.offsetY(), cfg.sectionHeight() + cfg.distanceBetweenSections()) - 1;
            } else return null;
        }

        return new Point(sectionX + sectionY * cfg.sectionWidth());
    }

    @Override
    public Point getStart() {
        return start;
    }

    @Override
    public Point getEnd() {
        return end;
    }

    private void addText(String content, Point position, boolean preferStart, Element1D element) {
        int x = getCanvasX(position, preferStart);
        int y = getCanvasY(position, preferStart);
        Text text = new Text(content);

        text.setTranslateX(x - cfg.width() / 2);
        text.setTranslateY(y - cfg.height() / 2 - text.getBoundsInLocal().getHeight() / 2);

        labels.put(element, text);

        ctx.root.getChildren().add(text);
    }

    private void drawInterval(Interval interval) {
        int startY = getSectionY(interval.start(), true);
        int endY = getSectionY(interval.end(), false);

        gc.setFill(cfg.intervalColor());

        for (int Y = startY; Y <= endY; Y++) {
            int x = Y == startY
                    ? getCanvasX(interval.start(), true)
                    : getCanvasX(start, true);
            int width = Y == endY
                    ? getCanvasX(interval.end(), false) - x
                    : Y == startY
                        ? cfg.sectionStart() + cfg.sectionWidth() - x
                        : cfg.sectionWidth();

            gc.fillRect(
                    x,
                    cfg.offsetY() + Y * (cfg.sectionHeight() + cfg.distanceBetweenSections()),
                    width,
                    cfg.sectionHeight());
        }

        addText(interval);
        drawPoint(interval.start(), true);
        drawPoint(interval.end(), false);
    }

    private void clearInterval(Interval interval) {
        // whole interval is cleared although the first one pixel wide line is explicitly omitted (bug?)
        int startY = getSectionY(interval.start(), true);
        int endY = getSectionY(interval.end(), false);

        for (int Y = startY; Y <= endY; Y++) {
            int x = Y == startY
                    ? getCanvasX(interval.start(), true)
                    : getCanvasX(start, true);
            int width = Y == endY
                    ? getCanvasX(interval.end(), false) - x
                    : Y == startY
                        ? cfg.sectionStart() + cfg.sectionWidth() - x
                        : cfg.sectionWidth();

            gc.clearRect(
                    x + 1,
                    cfg.offsetY() + Y * (cfg.sectionHeight() + cfg.distanceBetweenSections()),
                    width,
                    cfg.sectionHeight());
        }

        clearText(interval);
        clearPoint(interval.end(), false);
    }

    private void drawPoint(Point point, boolean preferStart) {
        int x = getCanvasX(point, preferStart);
        int y = getCanvasY(point, preferStart);
        gc.setFill(cfg.pointColor());
        gc.fillRect(x, y, 1, cfg.sectionHeight());
    }

    private void clearPoint(Point point) {
        clearPoint(point, true);
        clearPoint(point, false);
    }

    private void clearPoint(Point point, boolean preferStart) {
        int x = getCanvasX(point, preferStart);
        int y = getCanvasY(point, preferStart);
        gc.clearRect(x, y, 1, cfg.sectionHeight());
    }

    private int getSectionX(Point point, boolean preferStart) {
        return preferStart && !point.equals(end)
                ? point.center() % cfg.sectionWidth()
                : ((point.center() - 1) % cfg.sectionWidth()) + 1;
    }

    private int getSectionY(Point point, boolean preferStart) {
        return preferStart && !point.equals(end)
                ? point.center() / cfg.sectionWidth()
                : Math.max(0, point.center() - 1) / cfg.sectionWidth();
    }

    private int getCanvasX(Point point, boolean preferStart) {
        return cfg.sectionStart() + getSectionX(point, preferStart);
    }

    private int getCanvasY(Point point, boolean preferStart) {
        return cfg.offsetY() + getSectionY(point, preferStart) * (cfg.sectionHeight() + cfg.distanceBetweenSections());
    }

    private int sectionCount() {
        return sectionCount(cfg.height());
    }

    private int sectionCount(int height) {
        return height > cfg.offsetY()
                ? (height - cfg.offsetY() - 1) % (cfg.sectionHeight() + cfg.distanceBetweenSections()) >= cfg.sectionHeight() - 1
                    ? ceilDiv(height - cfg.offsetY(), cfg.sectionHeight() + cfg.distanceBetweenSections())
                    : (height - cfg.offsetY()) / (cfg.sectionHeight() + cfg.distanceBetweenSections())
                : 0;
    }

    private static int ceilDiv(int x, int n) {
        return (x + n - 1) / n;
    }
}
