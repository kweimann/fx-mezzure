package kweimann;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import kweimann.mezzure.IntervalsEventHandler;
import kweimann.mezzure.IntervalsGraphicsContext;

import static kweimann.mezzure.IntervalsUtil.height;
import static kweimann.mezzure.IntervalsUtil.width;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        Canvas canvas = new Canvas(width, height);
        StackPane stackPane = new StackPane(canvas);
        ScrollPane scrollPane = new ScrollPane(stackPane);
        Scene scene = new Scene(scrollPane, width + 15, 800);
        IntervalsGraphicsContext igc = new IntervalsGraphicsContext(canvas, stackPane);

        igc.initializeCanvas();

        canvas.addEventHandler(MouseEvent.ANY, new IntervalsEventHandler(igc));

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
