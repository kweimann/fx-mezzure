package kweimann.mezzure.model;

public interface Element1D {
    /* returns new element moved n units right if n >= 0 or n units left if n < 0 */
    Element1D move(int n);

    /* returns rounded down center of the element as X coordinate */
    int center();

    /* returns length of the element */
    int length();

    /* returns true if two elements overlap */
    default boolean overlaps(Element1D element) {
        int thisCenter = this.center() * 2 + (this.length() % 2 == 0 ? 0 : 1);
        int thatCenter = element.center() * 2 + (element.length() % 2 == 0 ? 0 : 1);
        return Math.abs(thatCenter - thisCenter) <= element.length() + this.length();
    }

    /* returns the smallest displacement between the most outer points of this element and another element */
    default int displacement(Element1D element) {
        int thisCenter = this.center() * 2 + (this.length() % 2 == 0 ? 0 : 1);
        int thatCenter = element.center() * 2 + (element.length() % 2 == 0 ? 0 : 1);
        if (Math.abs(thatCenter - thisCenter) > element.length() + this.length()) {
            return thatCenter >= thisCenter
                    // this element lies left to another element
                    ? ((thatCenter - element.length()) - (thisCenter + this.length())) / 2
                    // this element lies right to another element
                    : ((thatCenter + element.length()) - (thisCenter - this.length())) / 2;
        } else return 0;
    }

    /* returns the smallest distance between the most outer points of this element and another element */
    default int distance(Element1D element) {
        return Math.abs(displacement(element));
    }
}
