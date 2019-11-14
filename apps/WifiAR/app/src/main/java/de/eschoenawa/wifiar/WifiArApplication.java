package de.eschoenawa.wifiar;

import android.app.Application;
import android.util.Log;

import de.eschoenawa.wifiar.utils.Preferences;
import de.eschoenawa.wifiar.utils.WifiDataCollector;

public class WifiArApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Preferences
        Preferences.init(getApplicationContext());

        // Init WifiDataCollector
        if (!WifiDataCollector.getInstance().initialize(getApplicationContext())) {
            Log.e("WifiAR", "Unable to initialize WifiDataCollector!");
        }
    }
}
