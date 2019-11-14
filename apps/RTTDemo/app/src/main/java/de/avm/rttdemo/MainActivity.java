package de.avm.rttdemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.rtt.RangingRequest;
import android.net.wifi.rtt.RangingResult;
import android.net.wifi.rtt.RangingResultCallback;
import android.net.wifi.rtt.WifiRttManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 42;

    private RttResultAdapter adapter;
    private WifiRttManager rttManager;
    private WifiManager wifiManager;
    private Map<String, String> apNameMap;

    @SuppressLint("WrongConstant")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
        RecyclerView list = findViewById(R.id.resultList);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RttResultAdapter(this);
        list.setAdapter(adapter);

        Context context = getApplicationContext();
        rttManager = (WifiRttManager) context.getSystemService(Context.WIFI_RTT_RANGING_SERVICE);
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                if (intent.getAction() != null && intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                    scanResultsAvailable();
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(wifiScanReceiver, intentFilter);
    }

    public void onClickStart(View v) {
        wifiManager.startScan();
    }

    private void scanResultsAvailable() {
        Toast.makeText(this, "WLAN-Umgebung gescannt, starte RTT...", Toast.LENGTH_SHORT).show();
        List<ScanResult> scanResults = wifiManager.getScanResults();
        List<ScanResult> newScanResults = new ArrayList<>();
        // Add devices with 11mc support first
        Iterator<ScanResult> iterator = scanResults.iterator();
        while (iterator.hasNext()) {
            ScanResult scanResult = iterator.next();
            if (scanResult.is80211mcResponder()) {
                newScanResults.add(scanResult);
                iterator.remove();
            }
        }
        // Fill remaining slots (if any) with devices without 11mc support
        while (newScanResults.size() < RangingRequest.getMaxPeers()) {
            newScanResults.add(scanResults.remove(0));
        }
        // Use scanResults variable to create sublist cut to maximum number of peers (should only remove scanResults if more than RangingRequest.getMaxPeers() devices support 11mc)
        scanResults = newScanResults.subList(0, Math.min(newScanResults.size(), RangingRequest.getMaxPeers()));

        apNameMap = createMacToApMap(scanResults);
        RangingRequest.Builder builder = new RangingRequest.Builder();
        builder.addAccessPoints(scanResults);
        RangingRequest request = builder.build();
        if (rttManager.isAvailable()) {
            rttManager.startRanging(request, this.getMainExecutor(), new RangingResultCallback() {

                @Override
                public void onRangingFailure(int i) {
                    Toast.makeText(MainActivity.this, "RTT fehlgeschlagen!", Toast.LENGTH_SHORT).show();
                    adapter.setResults(new ArrayList<RangingResult>(), apNameMap);
                }

                @Override
                public void onRangingResults(List<RangingResult> list) {
                    Toast.makeText(MainActivity.this, "RTT erfolgreich!", Toast.LENGTH_SHORT).show();
                    adapter.setResults(list, apNameMap);
                }
            });
        } else {
            Toast.makeText(this, "RTT nicht auf dem Gerät verfügbar!", Toast.LENGTH_SHORT).show();
        }
    }

    private Map<String, String> createMacToApMap(List<ScanResult> scanResults) {
        Map<String, String> result = new HashMap<>();
        for (ScanResult scanResult : scanResults) {
            result.put(scanResult.BSSID.toUpperCase(Locale.US), scanResult.SSID);
        }
        return result;
    }
}
