package kweimann.mezzure.view;

import javafx.scene.paint.Color;

public final class MezzureConfiguration {
    private static final int DEFAULT_SECTION_START = 0;
    private static final int DEFAULT_OFFSET_Y = 0;
    private static final int DEFAULT_DISTANCE_BETWEEN_SECTIONS = 0;
    private static final Color DEFAULT_INTERVAL_COLOR = Color.rgb(0, 224, 222, 0.2);
    private static final Color DEFAULT_POINT_COLOR = Color.rgb(0, 0, 0, 0.25);

    private final int width;
    private final int height;
    private final int sectionWidth;
    private final int sectionHeight;
    private final int sectionStart;
    private final int offsetY;
    private final int distanceBetweenSections;
    private final Color intervalColor;
    private final Color pointColor;

    private MezzureConfiguration(Builder builder) {
        this.width = builder.width;
        this.height = builder.height;
        this.sectionWidth = builder.sectionWidth;
        this.sectionHeight = builder.sectionHeight;
        this.sectionStart = builder.sectionStart;
        this.offsetY = builder.offsetY;
        this.distanceBetweenSections = builder.distanceBetweenSections;
        this.intervalColor = builder.intervalColor;
        this.pointColor = builder.pointColor;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int sectionWidth() {
        return sectionWidth;
    }

    public int sectionHeight() {
        return sectionHeight;
    }

    public int sectionStart() {
        return sectionStart;
    }

    public int offsetY() {
        return offsetY;
    }

    public int distanceBetweenSections() {
        return distanceBetweenSections;
    }

    public Color intervalColor() {
        return intervalColor;
    }

    public Color pointColor() {
        return pointColor;
    }

    public static Builder builder(int width, int height) {
        return new Builder(width, height);
    }

    public static final class Builder {
        private final int width;
        private final int height;
        private int sectionWidth;
        private int sectionHeight;
        private int sectionStart                = DEFAULT_SECTION_START;
        private int offsetY                     = DEFAULT_OFFSET_Y;
        private int distanceBetweenSections     = DEFAULT_DISTANCE_BETWEEN_SECTIONS;
        private Color intervalColor             = DEFAULT_INTERVAL_COLOR;
        private Color pointColor                = DEFAULT_POINT_COLOR;

        private Builder(int width, int height) {
            this.width = width;
            this.height = height;
            this.sectionHeight = height;
            this.sectionWidth = width;
        }

        public Builder sectionHeight(int sectionHeight) {
            this.sectionHeight = sectionHeight;
            return this;
        }

        public Builder sectionWidth(int sectionWidth) {
            this.sectionWidth = sectionWidth;
            return this;
        }

        public Builder sectionStart(int sectionStart) {
            this.sectionStart = sectionStart;
            return this;
        }

        public Builder offsetY(int offsetY) {
            this.offsetY = offsetY;
            return this;
        }

        public Builder distanceBetweenSections(int distanceBetweenSections) {
            this.distanceBetweenSections = distanceBetweenSections;
            return this;
        }

        public Builder intervalColor(Color intervalColor) {
            this.intervalColor = intervalColor;
            return this;
        }

        public Builder pointColor(Color pointColor) {
            this.pointColor = pointColor;
            return this;
        }

        public MezzureConfiguration build() {
            return new MezzureConfiguration(this);
        }
    }
}
