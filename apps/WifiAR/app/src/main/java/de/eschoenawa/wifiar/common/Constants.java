package de.eschoenawa.wifiar.common;

public class Constants {
    public static final float INDICATOR_HEIGHT = 0.2f;
    public static final float FUZZY_TARGET_DETECTION_DISTANCE = 0.1f;

    public static final double INVALID_RSSI = 0;
    public static final double FUZZY_WIDTH = 0.000000001;
    public static final int MAX_VALUES_PER_THREAD = 10000;
    public static final int THREAD_COUNT = 12;
    public static final double MAX_MEASURE_DISTANCE = 0.3;
    public static final long MILLIS_BETWEEN_SCANS = 1000;

    public static final double MAX_POWER = 0.0001;
    public static final double MIN_POWER = 0;
    public static final double MIN_POWER_DBM = -100;

    // use for bitmap export / share
    public static final String BITMAP_PATH = "/wifiar/Heatmaps/";

    // Unique names
    public static final String SHARED_PREFERENCES_NAME = "de.eschoenawa.wifiar.MAIN_PREFERENCES";

    // Setting fields
    public static final String SETTING_DEBUG_LINES = "debug_lines";
    public static final String SETTING_PIXEL_PER_METER = "pixel_meter";
    public static final String SETTING_GRADIENT_WIDTH = "gradient_width";
    public static final String SETTING_HEATMAP_MODE = "heatmap_mode";
    public static final String SETTING_HEATMAP_COLOR = "heatmap_color";
    public static final String SETTING_ACCURATE_RSSI = "accurate_rssi";
    public static final String SETTING_NUMBER_OF_MEASUREMENTS = "number_of_measurements";
    public static final String SETTING_AUTO_AREA = "auto_area";
}
