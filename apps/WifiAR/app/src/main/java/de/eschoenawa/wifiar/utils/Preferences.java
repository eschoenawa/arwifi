package de.eschoenawa.wifiar.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import de.eschoenawa.wifiar.common.Constants;
import de.eschoenawa.wifiar.exceptions.NotInitializedException;
import de.eschoenawa.wifiar.heatmap.ColorSelector;

/**
 * This utility-class provides methods for storing and retrieving primitives to persist settings
 * and certain flags.
 *
 * @author Emil Schoenawa
 */
public class Preferences {

    private class Fields {
        private static final String TIP_AREA_POINT = "tip_area_point";
        private static final String TIP_AREA_DONE = "tip_area_done";
        private static final String TIP_MAKE_MEASUREMENT = "tip_make_measurement";
        private static final String TIP_GENERATE = "tip_generate";

        // field names of settings are located in the Constants-class
    }

    private static Preferences INSTANCE;
    private SharedPreferences sharedPreferences;
    private SharedPreferences settings;

    private Preferences(Context context) {
        this.sharedPreferences = context.getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        this.settings = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public synchronized static void init(Context context) {
        if (isNotInitialized()) {
            INSTANCE = new Preferences(context.getApplicationContext());
        }

    }

    public static Preferences getInstance() {
        checkInitialized();
        return INSTANCE;
    }

    // Flags

    public boolean wasNewAreaPointTipShown() {
        return this.sharedPreferences.getBoolean(Fields.TIP_AREA_POINT, false);
    }

    public void setNewAreaPointTipShown(boolean shown) {
        this.sharedPreferences.edit().putBoolean(Fields.TIP_AREA_POINT, shown).apply();
    }

    public boolean wasAreaDoneTipShown() {
        return this.sharedPreferences.getBoolean(Fields.TIP_AREA_DONE, false);
    }

    public void setAreaDoneTipShown(boolean shown) {
        this.sharedPreferences.edit().putBoolean(Fields.TIP_AREA_DONE, shown).apply();
    }

    public boolean wasMeasureTipShown() {
        return this.sharedPreferences.getBoolean(Fields.TIP_MAKE_MEASUREMENT, false);
    }

    public void setMeasureTipShown(boolean shown) {
        this.sharedPreferences.edit().putBoolean(Fields.TIP_MAKE_MEASUREMENT, shown).apply();
    }

    public boolean wasGenerateTipShown() {
        return this.sharedPreferences.getBoolean(Fields.TIP_GENERATE, false);
    }

    public void setGenerateTipShown(boolean shown) {
        this.sharedPreferences.edit().putBoolean(Fields.TIP_GENERATE, shown).apply();
    }

    // Settings
    public boolean shouldDrawDebugLines() {
        return settings.getBoolean(Constants.SETTING_DEBUG_LINES, false);
    }

    public int getPixelsPerMeter() {
        String value = settings.getString(Constants.SETTING_PIXEL_PER_METER, "100");
        return Integer.parseInt(value);
    }

    public int getGradientWidth() {
        String value = settings.getString(Constants.SETTING_GRADIENT_WIDTH, "100");
        return Integer.parseInt(value);
    }

    public void setGradientWidth(int gradientWidth) {
        this.settings.edit().putString(Constants.SETTING_GRADIENT_WIDTH, Integer.toString(gradientWidth)).apply();
    }

    public ColorSelector.Mode getColorSelectionMode() {
        String value = settings.getString(Constants.SETTING_HEATMAP_MODE, "BOUNDS");
        return ColorSelector.getMode(value);
    }

    public int getHeatmapColors() {
        String value = settings.getString(Constants.SETTING_HEATMAP_COLOR, "0");
        return Integer.parseInt(value);
    }

    public boolean shouldWaitForNewWifiScan() {
        return settings.getBoolean(Constants.SETTING_ACCURATE_RSSI, false);
    }

    public int getNumberOfMeasurements() {
        String value = settings.getString(Constants.SETTING_NUMBER_OF_MEASUREMENTS, "3");
        int number = Integer.parseInt(value);
        if (number < 1 || number > 9) {
            number = 3;
            this.settings.edit().putString(Constants.SETTING_NUMBER_OF_MEASUREMENTS, "3").apply();
            Log.w("PREF", "Corrected number of measurements to default value (3) as it was outside the allowed range (0 < x < 10).");
        }
        return number;
    }

    public boolean isAreaAutoDefinitionEnabled() {
        return settings.getBoolean(Constants.SETTING_AUTO_AREA, false);
    }

    // Helper

    private static boolean isNotInitialized() {
        return INSTANCE == null;
    }

    private static void checkInitialized() {
        if (isNotInitialized()) {
            throw new NotInitializedException("Please initialize the Preferences first!");
        }
    }
}
