package de.eschoenawa.wifiar.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.util.Log;

import de.eschoenawa.wifiar.utils.WifiDataCollector;

public class WifiUpdateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        Log.d("WifiUpdateReceiver", "Received Broadcast with action '" + action + '!');
        if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
            WifiDataCollector wifiDataCollector = WifiDataCollector.getInstance();
            if (wifiDataCollector.isInitialized()) {
                wifiDataCollector.scanResultsAvailable();
            }
        }
    }
}
