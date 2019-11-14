package de.eschoenawa.wifiar.utils;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import de.eschoenawa.wifiar.exceptions.NotInitializedException;
import de.eschoenawa.wifiar.receiver.WifiUpdateReceiver;

import static android.net.wifi.WifiManager.SCAN_RESULTS_AVAILABLE_ACTION;

/**
 * This class allows the retrieval of information about the currently connected wifi.
 */
public class WifiDataCollector {

    private WifiManager wifiManager;
    private ScanResultsAvailableListener scanResultsAvailableListener;
    private BroadcastReceiver scanResultsBroadcastReceiver;

    /**
     * Helper-class to make WifiDataCollector a Bill Pugh Singleton.
     */
    private static class InstanceHolder {
        private static final WifiDataCollector INSTANCE = new WifiDataCollector();
    }

    public static WifiDataCollector getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private WifiDataCollector() {

    }

    public synchronized boolean initialize(Context context) {
        this.wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        return this.wifiManager != null;
    }

    public boolean isInitialized() {
        return this.wifiManager != null;
    }

    public double getCurrentRssiInWatts() {
        return UnitConverter.dbmToWatts(getCurrentRssiInDbm());
    }

    public double getCurrentRssiInDbm() {
        checkInitialized();
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        return wifiInfo.getRssi();
    }

    public double getCurrentFrequency() {
        checkInitialized();
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        return UnitConverter.megaToUnit(wifiInfo.getFrequency());
    }

    public boolean doesDeviceSupportRtt() {
        checkInitialized();
        return wifiManager.isDeviceToApRttSupported();
    }

    public void scanResultsAvailable() {
        if (this.scanResultsAvailableListener != null) {
            this.scanResultsAvailableListener.onScanResultsAvailable();
        }
    }

    public void setScanResultsAvailableListener(ScanResultsAvailableListener scanResultsAvailableListener) {
        this.scanResultsAvailableListener = scanResultsAvailableListener;
    }

    public void startScan() {
        this.wifiManager.startScan();
    }

    public void startReceivingScanResults(Activity activity) {
        scanResultsBroadcastReceiver = new WifiUpdateReceiver();
        IntentFilter filter = new IntentFilter(SCAN_RESULTS_AVAILABLE_ACTION);
        activity.registerReceiver(scanResultsBroadcastReceiver, filter);
    }

    public void stopReceivingScanResults(Activity activity) {
        activity.unregisterReceiver(scanResultsBroadcastReceiver);
        scanResultsBroadcastReceiver = null;
    }

    private void checkInitialized() throws NotInitializedException {
        if (!isInitialized()) {
            throw new NotInitializedException("Please initialize the WifiDataCollector first!");
        }
    }

    public interface ScanResultsAvailableListener {
        void onScanResultsAvailable();
    }
}
