package kweimann;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import kweimann.mezzure.view.Mezzure;
import kweimann.mezzure.view.MezzureConfiguration;

import javafx.scene.paint.Color;
import kweimann.mezzure.controller.MezzureEventHandler;
import kweimann.mezzure.model.Interval;
import kweimann.mezzure.util.Collections;
import kweimann.mezzure.util.Window;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;

public class Main extends Application {
    private static final int sectionCount = 5;

    private static final PlotData data = new PlotData(
            x -> Math.cos(x) + Math.sin(Math.sqrt(x)),
            0,
            Math.PI * 32 * sectionCount,
            10000);

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws Exception {
        MezzureConfiguration configuration = MezzureConfiguration
                .builder(1000, 50 + sectionCount * 175)
                .sectionStart(100)
                .sectionWidth(800)
                .sectionHeight(125)
                .distanceBetweenSections(50)
                .offsetY(50)
                .build();

        StackPane stackPane = new StackPane();
        ScrollPane pane = new ScrollPane(stackPane);

        // prepare background canvas (may be any kind of node e.g. image view)
        Canvas background = getBackground(configuration);

        // Mezzure requires access to its pane parent in order to add/remove UI elements
        Mezzure mezzure = new Mezzure(stackPane, configuration);

        stackPane.getChildren().addAll(background, mezzure);

        // use normalizer to translate canvas length into plot length
        Normalizer normalizer = new Normalizer(
                data.xLow,
                data.xHigh,
                0,
                configuration.sectionWidth() * sectionCount);

        MezzureEventHandler handler = new MezzureEventHandler(mezzure.getDrawingContext());

        // listen for interval changes
        handler.setListener(((oldInterval, newInterval) -> System.out.println(oldInterval + " -> " + newInterval)));

        // set up the event handler
        mezzure.addEventHandler(MouseEvent.ANY, handler);

        // set up the interval labeling function
        mezzure.setDescriptor(e -> e instanceof Interval
                ? String.format("%.2f", normalizer.normalize(e.length()))
                : null
        );

        stage.setTitle("y = cos(x) + sin(sqrt(x))");
        stage.setScene(new Scene(pane, configuration.width() + 15, 800));
        stage.show();
    }

    private Canvas getBackground(MezzureConfiguration configuration) {
        Canvas canvas = new Canvas(configuration.width(), configuration.height());

        GraphicsContext gc = canvas.getGraphicsContext2D();

        double xTick = (Main.data.xHigh - Main.data.xLow) / sectionCount;

        for (int i = 0; i < sectionCount; i++) {
            PlotData data = new PlotData(
                    Main.data.function,
                    Main.data.xLow + i * xTick,
                    Main.data.xLow + (i + 1) * xTick,
                    Main.data.yLow,
                    Main.data.yHigh,
                    Main.data.nTicks / sectionCount);

            Rectangle box = new Rectangle(
                    configuration.sectionStart(),
                    configuration.offsetY() + i * (configuration.sectionHeight() + configuration.distanceBetweenSections()),
                    configuration.sectionWidth(),
                    configuration.sectionHeight());

            gc.setFill(Color.BLACK.deriveColor(0, 1, 1, 0.05));
            gc.fillRect(box.x, box.y ,box.width, box.height);

            gc.setFill(Color.GRAY);
            gc.setStroke(Color.LIGHTGRAY);
            drawBoxXAxis(gc, data, box, 16);
            drawBoxYAxis(gc, data, box, 3);

            gc.setStroke(Color.ORANGE.deriveColor(0, 1, 1, 0.7));
            drawFunction(gc, data, box);
        }

        return canvas;
    }

    private void drawFunction(GraphicsContext gc, PlotData data, Rectangle box) {
        Normalizer xNorm = new Normalizer(
                0,
                box.width,
                data.xLow,
                data.xHigh);

        Normalizer yNorm = new Normalizer(
                0,
                box.height,
                data.yLow,
                data.yHigh);

        for (Window<PlotData.Point> window : Collections.slideWindow(getPoints(data), 2)) {
            PlotData.Point a = window.get(0);
            PlotData.Point b = window.get(1);

            gc.strokeLine(
                    box.x + xNorm.normalize(a.x),
                    box.y + box.height - yNorm.normalize(a.y),
                    box.x + xNorm.normalize(b.x),
                    box.y + box.height - yNorm.normalize(b.y));
        }
    }

    private void drawBoxXAxis(GraphicsContext gc, PlotData data, Rectangle box, int ticks) {
        Normalizer xNorm = new Normalizer(
                0,
                box.width,
                data.xLow,
                data.xHigh);

        double xTick = (data.xHigh - data.xLow) / (ticks - 1);

        gc.strokeLine(box.x, box.y + box.height, box.x + box.width, box.y + box.height);

        for (int tick = 0; tick < ticks; tick++) {
            double x = data.xLow + tick * xTick;
            double boxX = xNorm.normalize(x);
            Text text = new Text(String.format("%.2f", x));

            gc.fillRect(
                    box.x + (int) boxX,
                    box.y + box.height - 5,
                    1,
                    10
            );

            gc.fillText(
                    text.getText(),
                    box.x + boxX - text.getBoundsInLocal().getWidth() / 2,
                    box.y + box.height + text.getBoundsInLocal().getHeight() + 5);
        }
    }

    private void drawBoxYAxis(GraphicsContext gc, PlotData data, Rectangle box, int ticks) {
        Normalizer yNorm = new Normalizer(
                0,
                box.height,
                data.yLow,
                data.yHigh);

        double yTick = (data.yHigh - data.yLow) / (ticks - 1);

        gc.strokeLine(box.x, box.y, box.x, box.y + box.height);

        for (int tick = 0; tick < ticks; tick++) {
            double y = data.yLow + tick * yTick;
            double boxY = yNorm.normalize(y);
            Text text = new Text(String.format("%.2f", y));

            gc.fillRect(
                    box.x - 5,
                    box.y + box.height - (int) boxY,
                    10,
                    1
            );

            gc.fillText(
                    text.getText(),
                    box.x - text.getBoundsInLocal().getWidth() - 10,
                    box.y + box.height - boxY + text.getBoundsInLocal().getHeight() / 2 - 1);
        }
    }

    private Iterable<PlotData.Point> getPoints(PlotData data) {
        return () -> new Iterator<PlotData.Point>() {

            int tick = 0;

            @Override
            public boolean hasNext() {
                return tick < data.nTicks;
            }

            @Override
            public PlotData.Point next() {
                if (tick >= data.nTicks) throw new NoSuchElementException();
                double x = data.xLow + tick++ * data.xTick;
                return new PlotData.Point(x, data.function.apply(x));
            }
        };
    }

    private static class Normalizer {

        final double slope;
        final double intercept;

        Normalizer(double scaleMin, double scaleMax, double dataMin, double dataMax) {
            this.slope = (scaleMax - scaleMin) / (dataMax - dataMin);
            this.intercept = scaleMax - this.slope * dataMax;
        }

        double normalize(double n) {
            return slope * n + intercept;
        }
    }

    private static class PlotData {
        static class Point {
            final double x;
            final double y;

            Point(double x, double y) {
                this.x = x;
                this.y = y;
            }
        }

        final Function<Double, Double> function;
        final double xLow;
        final double xHigh;
        final double yLow;
        final double yHigh;
        final int nTicks;
        final double xTick;

        PlotData(Function<Double, Double> function, double xLow, double xHigh, double yLow, double yHigh, int nTicks) {
            if (xLow > xHigh) throw new IllegalArgumentException();
            if (nTicks < 2) throw new IllegalArgumentException();

            this.function = function;
            this.xLow = xLow;
            this.xHigh = xHigh;
            this.yLow = yLow;
            this.yHigh = yHigh;
            this.nTicks = nTicks;
            this.xTick = (xHigh - xLow) / (nTicks - 1);
        }

        PlotData(Function<Double, Double> function, double xLow, double xHigh, int nTicks) {
            if (xLow > xHigh) throw new IllegalArgumentException();
            if (nTicks < 2) throw new IllegalArgumentException();

            double yLow = function.apply(xLow);
            double yHigh = function.apply(xLow);

            double xTick = (xHigh - xLow) / (nTicks - 1);
            for (int tick = 0; tick < nTicks; tick++) {
                double x = xLow + tick * xTick;
                double y = function.apply(x);
                yLow = Math.min(yLow, y);
                yHigh = Math.max(yHigh, y);
            }

            this.function = function;
            this.xLow = xLow;
            this.xHigh = xHigh;
            this.yLow = yLow;
            this.yHigh = yHigh;
            this.nTicks = nTicks;
            this.xTick = xTick;
        }
    }

    private static class Rectangle {
        final int x;
        final int y;
        final int width;
        final int height;

        Rectangle(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }
}
