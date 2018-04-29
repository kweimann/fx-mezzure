package kweimann.mezzure;

import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import java.util.HashMap;
import java.util.Map;

import static kweimann.mezzure.IntervalsUtil.*;

public class IntervalsGraphicsContext implements Painter {
    public static final Color defaultColor              = Color.GRAY;
    public static final Color intervalColor             = Color.TURQUOISE;
    public static final Color pointColor                = Color.CRIMSON;

    protected Canvas canvas;
    protected Pane root;
    protected Map<Element, Text> labels = new HashMap<>();

    public IntervalsGraphicsContext(Canvas canvas, Pane root) {
        this.canvas = canvas;
        this.root = root;
    }

    public void initializeCanvas() {
        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.setFill(defaultColor);

        for (int i = 0; i < sectionCount(); i++) {
            gc.fillRect(sectionStart, Y(i), sectionWidth(), sectionHeight);
        }
    }

    @Override
    public void clear(Element element) {
        paint(element, true);
    }

    @Override
    public void clearText(Element element) {
        root.getChildren().remove(labels.remove(element));
    }

    @Override
    public void paint(Element element) {
        paint(element, false);
    }

    @Override
    public void paintText(String content, Element element) {
        Text text = new Text(content);
        Point position;

        if (element instanceof Point) position = (Point) element;
        else if (element instanceof Interval) position = ((Interval) element).start();
        else throw new IllegalArgumentException("unsupported element");

        text.setTranslateX(X(position) - canvas.getWidth() / 2 + text.getBoundsInLocal().getWidth() / 2 + 1);
        text.setTranslateY(Y(position) - canvas.getHeight() / 2 - text.getBoundsInLocal().getHeight() / 2);

        labels.put(element, text);

        this.root.getChildren().add(text);
    }

    @Override
    public void setCursor(Cursor cursor) {
        canvas.setCursor(cursor);
    }

    private void paint(Element element, boolean clear) {
        GraphicsContext gc = canvas.getGraphicsContext2D();

        if (element instanceof Point) {
            Point point = (Point) element;

            int x = X(point);
            int y = Y(point);

            if (clear)
                gc.setFill(defaultColor);
            else
                gc.setFill(pointColor);

            gc.fillRect(x, y, 1, sectionHeight);
        } else if (element instanceof Interval) {
            Interval interval = (Interval) element;

            int startI = interval.startI();
            int endI = interval.endI();

            if (clear) {
                gc.setFill(defaultColor);
            } else {
                gc.setFill(intervalColor);
            }

            for (int i = interval.startI(); i <= interval.endI(); i++) {
                int startX = i == startI
                        ? interval.startX() == sectionWidth() ? sectionEnd
                        : X(interval.start()) : sectionStart;
                int width = i == endI
                        ? X(interval.end()) - startX
                        : i == startI ? sectionEnd - startX : sectionWidth();

                gc.fillRect(startX, Y(i), width, sectionHeight);
            }

            if (clear) {
                clearText(interval);

                clear(interval.start());
                clear(interval.end());
            } else {
                paintText(Integer.toString(interval.length()), interval);

                paint(interval.start());
                paint(interval.end());
            }
        } else throw new IllegalArgumentException("unsupported element");
    }
}

