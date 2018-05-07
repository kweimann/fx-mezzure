package kweimann.mezzure.view;

import javafx.scene.Cursor;
import kweimann.mezzure.model.Element1D;
import kweimann.mezzure.model.Point;

public interface DrawingContext {
    void addText(Element1D element);
    void clearText(Element1D element);

    void draw(Element1D element);
    void clear(Element1D element);

    void setCursor(Cursor cursor);

    Point getPoint(int canvasX, int canvasY, boolean fitToSection);
    Point getStart();
    Point getEnd();
}
