package de.eschoenawa.wifiar.heatmap;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.GradientDrawable;
import android.net.wifi.WifiManager;

import de.eschoenawa.wifiar.models.Polygon;
import de.eschoenawa.wifiar.utils.Preferences;
import de.eschoenawa.wifiar.utils.UnitConverter;
import io.github.jdiemke.triangulation.Vector2D;

public class ColorSelector {

    private Mode mode;
    private Bitmap gradient;
    private int gradientWidth;
    private double startValue;
    private double endValue;

    public enum Mode {
        BOUNDS, WIFI_BARS
    }

    /**
     * Creates a new ColorSelector based on the mode. If {@link Mode#BOUNDS} is chosen the gradient
     * width will be loaded from the preferences, otherwise it will be set to fit the given {@link Mode}.
     *
     * @param mode       The {@link Mode} to use for choosing the color
     * @param colors     The {@link GradientColors} to use
     * @param boundaries The lowest and highest possible value; these parameters will be ignored in
     *                   modes other  than {@link Mode#BOUNDS}
     */
    public ColorSelector(Mode mode, GradientColors colors, double... boundaries) {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColors(colors.getColors());
        gradientDrawable.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        gradientDrawable.setShape(GradientDrawable.RECTANGLE);
        gradientDrawable.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
        this.mode = mode;
        switch (this.mode) {
            case BOUNDS:
                if (boundaries == null || boundaries.length != 2) {
                    throw new IllegalArgumentException("Mode set to bounds but an invalid number of boundaries were given.");
                }
                this.startValue = boundaries[0];
                this.endValue = boundaries[1];
                this.gradientWidth = Preferences.getInstance().getGradientWidth();
                if (this.gradientWidth == -1) {
                    this.gradientWidth = colors.getColors().length;
                }
                break;
            case WIFI_BARS:
                this.startValue = this.endValue = Double.NaN;
                this.gradientWidth = colors.getColors().length;
                break;
        }
        gradientDrawable.setSize(this.gradientWidth, 1);
        gradientDrawable.setBounds(0, 0, this.gradientWidth, 1);
        this.gradient = Bitmap.createBitmap(this.gradientWidth, 1, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(this.gradient);
        gradientDrawable.draw(canvas);
    }

    public int getColorForValue(double value) {
        switch (this.mode) {
            default:
            case BOUNDS:
                double fraction = value / (startValue + endValue);
                return gradient.getPixel((int) (fraction * gradientWidth), 0);
            case WIFI_BARS:
                int bars = WifiManager.calculateSignalLevel((int) Math.round(UnitConverter.wattsToDbm(value)), this.gradientWidth);
                return gradient.getPixel(bars, 0);
        }
    }

    /**
     * This helper method allows finding the minimum and maximum values in a given heatmap.
     *
     * @param heatmap The heatmap of which the min/max should be determined
     * @return An array containing the min value at position 0 and the max at position 1
     */
    public static double[] findMinAndMaxValuesInHeatmap(Polygon area, double[][] heatmap) {
        double max = Double.MIN_VALUE;
        double min = Double.MAX_VALUE;
        double[] result = new double[2];
        for (int x = 0; x < heatmap.length; x++) {
            double[] heatmapRow = heatmap[x];
            for (int y = 0; y < heatmapRow.length; y++) {
                double heatmapCell = heatmapRow[y];
                if (area.isPointInPolygon(new Vector2D(x, y)) && !Double.isNaN(heatmapCell)) {
                    max = Math.max(max, heatmapCell);
                    min = Math.min(min, heatmapCell);
                }
            }
        }
        if (min <= max) {
            result[0] = min;
            result[1] = max;
            return result;
        } else {
            throw new IllegalArgumentException("Heatmap contains no valid numbers!");
        }
    }

    public static Mode getMode(String modeString) {
        switch (modeString) {
            case "BOUNDS":
            default:
                return Mode.BOUNDS;
            case "WIFI_BARS":
                return Mode.WIFI_BARS;
        }
    }
}
