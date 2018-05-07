package kweimann.mezzure.controller;

import kweimann.mezzure.model.Interval;

public interface ChangeListener {
    /* oldInterval == null => new interval created
     * newInterval == null => interval removed */

    void onChange(Interval oldInterval, Interval newInterval);

    default void onDrag(Interval oldInterval, Interval newInterval) {}
}
