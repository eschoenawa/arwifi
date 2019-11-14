package de.eschoenawa.wifiar.utils;

/**
 * This utility-class provides methods to convert between meter-based coordinates and pixel-based
 * coordinates.
 *
 * @author Emil Schoenawa
 */
public class ScaleHelper {
    public static double pixelsToMeters(int pixels) {
        return pixels / Preferences.getInstance().getPixelsPerMeter();
    }

    public static int metersToPixels(double meters) {
        return (int) Math.ceil(meters * Preferences.getInstance().getPixelsPerMeter());
    }
}
