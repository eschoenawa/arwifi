package de.eschoenawa.wifiar.heatmap;

import android.graphics.Color;

public class GradientColors {
    public static final int MODE_RED_YELLOW_GREEN = 0;
    public static final int MODE_GREEN_YELLOW_RED = 1;
    public static final int MODE_RED_YELLOW_GREEN_DARKGREEN = 2;
    public static final int MODE_RED_GREEN = 3;
    public static final int MODE_RED_YELLOW_TRANSPARENT = 4;

    private int[] colors;

    public GradientColors(int mode) {
        switch (mode) {
            // Default colors: red is worse than yellow and yellow is worse than green
            default:
            case MODE_RED_YELLOW_GREEN:
                this.colors = new int[]{
                        Color.RED,
                        Color.YELLOW,
                        Color.GREEN
                };
                break;
            case MODE_GREEN_YELLOW_RED:
                this.colors = new int[]{
                        Color.GREEN,
                        Color.YELLOW,
                        Color.RED
                };
                break;
            case MODE_RED_YELLOW_GREEN_DARKGREEN:
                this.colors = new int[]{
                        Color.RED,
                        Color.YELLOW,
                        Color.GREEN,
                        Color.argb(255, 0, 60, 0)
                };
                break;
            case MODE_RED_GREEN:
                this.colors = new int[]{
                        Color.RED,
                        Color.GREEN
                };
                break;
            case MODE_RED_YELLOW_TRANSPARENT:
                this.colors = new int[]{
                        Color.RED,
                        Color.YELLOW,
                        Color.argb(0, 255, 255, 0)
                };
                break;
        }
    }

    public int[] getColors() {
        return colors;
    }
}
