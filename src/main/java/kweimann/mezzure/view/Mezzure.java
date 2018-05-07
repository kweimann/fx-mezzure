package kweimann.mezzure.view;

import javafx.scene.canvas.Canvas;
import javafx.scene.layout.Pane;
import kweimann.mezzure.model.Element1D;

import java.util.function.Function;

public class Mezzure extends Canvas {

    final Pane root;
    final MezzureConfiguration configuration;
    Function<Element1D, String> descriptor;

    private final MezzureDrawingContext dc;

    public Mezzure(Pane root, MezzureConfiguration configuration) {
        super(configuration.width(), configuration.height());
        this.root = root;
        this.configuration = configuration;
        this.dc = new MezzureDrawingContext(this);
    }

    public MezzureDrawingContext getDrawingContext() {
        return dc;
    }

    public void setDescriptor(Function<Element1D, String> descriptor) {
        this.descriptor = descriptor;
    }
}
