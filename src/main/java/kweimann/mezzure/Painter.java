package kweimann.mezzure;

import javafx.scene.Cursor;

public interface Painter {
    void clear(Element element);
    void clearText(Element element);
    void paint(Element element);
    void paintText(String content, Element element);
    void setCursor(Cursor cursor);
}
