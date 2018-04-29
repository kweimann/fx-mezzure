package kweimann.mezzure;

public interface Element {
    Element move(int units);
    boolean overlaps(Element element);
}
