package de.eschoenawa.wifiar.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;

import de.eschoenawa.wifiar.R;
import de.eschoenawa.wifiar.controller.HeatmapGenerationController;
import de.eschoenawa.wifiar.controller.callback.HeatmapGenerationControllerCallback;
import de.eschoenawa.wifiar.utils.DialogHelper;
import de.eschoenawa.wifiar.utils.ImageHolder;
import de.eschoenawa.wifiar.utils.OnboardingHelper;
import de.eschoenawa.wifiar.utils.Preferences;
import de.eschoenawa.wifiar.views.ArWifiToolbar;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class ArRecordVisualizeActivity extends AppCompatActivity implements HeatmapGenerationControllerCallback {

    private static final String TAG = "ArActivity";
    private static final int RC_LOCATION_PERMISSION = 1337;
    private static final int RC_IN_APP_SETTINGS = 1338;
    // Controller
    private HeatmapGenerationController controller;

    // Views
    private TextView txtHint;
    private ProgressBar hintLoading;
    private ArWifiToolbar toolbar;
    private ImageView heatmapHoldingImageView;

    // Renderables
    private ViewRenderable heatmapRenderable;

    // Flags
    private boolean noAreaPointPlacedYet = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        // Set views
        txtHint = findViewById(R.id.txtHint);
        hintLoading = findViewById(R.id.hintLoading);
        toolbar = findViewById(R.id.toolbar);
        hintLoading.setMax(100);

        ArFragment arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.main_fragment);
        this.controller = new HeatmapGenerationController(arFragment, this, this);

        ViewRenderable.builder()
                .setView(this, R.layout.heatmap_holder)
                .build()
                .thenAccept(renderable -> {
                    heatmapRenderable = renderable;
                    heatmapRenderable.setPixelsToMetersRatio(Preferences.getInstance().getPixelsPerMeter());
                    heatmapHoldingImageView = heatmapRenderable.getView().findViewById(R.id.heatmapImageView);
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        controller.refreshAreaDesignationMode();
    }

    // Permission handling
    @NeedsPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    void requireLocationPermission() {
        Log.d(TAG, "Location permission is granted!");
    }

    @OnPermissionDenied(Manifest.permission.ACCESS_COARSE_LOCATION)
    void locationPermissionDenied() {
        Log.w(TAG, "Location permission was denied!");
        DialogHelper.showLocationRationaleDialog(this, (dialog, which) -> ArRecordVisualizeActivityPermissionsDispatcher.requireLocationPermissionWithPermissionCheck(ArRecordVisualizeActivity.this), (dialog, which) -> finish());
    }

    @OnNeverAskAgain(Manifest.permission.ACCESS_COARSE_LOCATION)
    void locationPermissionPermanentlyDenied() {
        Log.w(TAG, "Location permission was permanently denied!");
        DialogHelper.showLocationPermanentlyDeniedDialog(this, (dialog, which) -> openPermissionSettings(), (dialog, which) -> finish());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        ArRecordVisualizeActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Restart Activity if returning from settings
        if (requestCode == RC_IN_APP_SETTINGS) {
            restart();
        }
    }

    private void openPermissionSettings() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", this.getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, RC_LOCATION_PERMISSION);
    }

    // ClickListeners
    public void onClickUndo(View view) {
        this.controller.performUndo();
    }

    public void onClickAddPoint(View view) {
        this.controller.addAreaPoint();
    }

    public void onClickMeasure(View view) {
        this.controller.measureAtCurrentPosition();
    }

    public void onClickFinish(View view) {
        this.controller.startHeatmapGeneration();
    }

    public void onClickSettings(View view) {
        if (noAreaPointPlacedYet) {
            startActivityForResult(new Intent(this, SettingsActivity.class), RC_IN_APP_SETTINGS);
        } else {
            // ARCore may be able to keep tracking the already placed objects, but not reliably and
            // not always. To prevent inconsistent behaviour this activity is restarted when returning
            // from the settings. Show a warning if the user has already made some progress.
            DialogHelper.showResetWarning(this, (dialog, which) -> startActivityForResult(new Intent(ArRecordVisualizeActivity.this, SettingsActivity.class), RC_IN_APP_SETTINGS));
        }
    }

    public void onClickTipText(View view) {
        controller.tipTextClicked();
    }

    public void onClickRestart(View view) {
        restart();
    }

    public void onClickCreateImage(View view) {
        Intent intent = new Intent(this, HeatmapImageActivity.class);
        // Use singleton to store image as it can exceed the Extra-Size-Limit of Intents
        ImageHolder.getInstance().setImage(this.controller.getHeatmap());
        startActivity(intent);
    }

    // Controller callbacks
    @Override
    public void setProgress(double percentage) {
        setHintLoadingProgressBasedOnPercentage(percentage);
    }

    @Override
    public void onPlaneDetected() {
        runOnUiThread(() -> {
            toolbar.setVisibility(View.VISIBLE);
            txtHint.setText(R.string.hint_area_placement);
            OnboardingHelper.newAreaPointTip(this);
        });
    }

    @Override
    public void onAllowUndo() {
        toolbar.onAllowUndo();
        noAreaPointPlacedYet = false;
    }

    @Override
    public void onDenyUndo() {
        toolbar.onDenyUndo();
        txtHint.setText(Preferences.getInstance().isAreaAutoDefinitionEnabled() ? R.string.hint_measure_auto_area : R.string.hint_area_placement);
        noAreaPointPlacedYet = true;
    }

    @Override
    public void onDrawAreaState(boolean areaCanBeFinished) {
        runOnUiThread(() -> {
            toolbar.onDrawAreaState();
            txtHint.setText(areaCanBeFinished ? R.string.hint_area_can_be_finished : R.string.hint_area_placement);
        });
    }

    @Override
    public void onAreaCompleted() {
        runOnUiThread(() -> {
            toolbar.onAreaCompleted();
            OnboardingHelper.areaDoneTip(this);
        });
    }

    @Override
    public void onEnterMeasureState() {
        runOnUiThread(() -> {
            toolbar.onEnterMeasureState();
            setHintLoadingProgressBasedOnPercentage(0);
            txtHint.setText(Preferences.getInstance().isAreaAutoDefinitionEnabled() ? R.string.hint_measure_auto_area : R.string.hint_go_to_measurement_location);
            OnboardingHelper.measureTip(this);
        });
    }

    @Override
    public void onMeasurementStart() {
        runOnUiThread(() -> {
            toolbar.onMeasurementStart();
            txtHint.setText(R.string.hint_wait_for_measure_end);
        });
    }

    @Override
    public void onAllowGeneration() {
        runOnUiThread(() -> {
            toolbar.onAllowGeneration();
            OnboardingHelper.generateTip(this);
        });
    }

    @Override
    public void onDenyGeneration() {
        runOnUiThread(() -> toolbar.onDenyGeneration());
    }

    @Override
    public void onGenerationStarted() {
        runOnUiThread(() -> {
            toolbar.onGeneration();
            hintLoading.setVisibility(View.VISIBLE);
            hintLoading.setIndeterminate(true);
            txtHint.setText(R.string.hint_creating_heatmap);
        });
    }

    @Override
    public void onRenderStarted() {
        runOnUiThread(() -> {
            setHintLoadingProgressBasedOnPercentage(0);
            txtHint.setText(R.string.hint_rendering_heatmap);
        });
    }

    @Override
    public void onRenderFinished() {
        runOnUiThread(() -> {
            hintLoading.setVisibility(View.GONE);
            setHintLoadingProgressBasedOnPercentage(0);
            txtHint.setText(R.string.hint_heatmap_done);
            toolbar.onHeatmapDisplay();
        });
    }

    @Override
    public Activity getActivity() {
        return this;
    }

    @Override
    public ViewRenderable getHeatmapRenderable() {
        return heatmapRenderable;
    }

    @Override
    public ImageView getHeatmapImageView() {
        return heatmapHoldingImageView;
    }

    @Override
    public void uiOperation(Runnable runnable) {
        runOnUiThread(runnable);
    }

    @Override
    public void requestLocationPermission() {
        runOnUiThread(() -> {
            // Request permission here as request on startup (in onCreate(), onStart() and onResume()) doesn't work
            // due to the ARFragment consuming the permission grant results
            ArRecordVisualizeActivityPermissionsDispatcher.requireLocationPermissionWithPermissionCheck(this);
        });
    }

    // Helper methods
    private void setHintLoadingProgressBasedOnPercentage(double percentage) {
        runOnUiThread(() -> {
            int progress = (int) Math.round(percentage);
            hintLoading.setProgress(progress);
            hintLoading.setIndeterminate(progress == 0);
        });
    }

    private void restart() {
        Intent intent = getIntent();
        finish();
        startActivity(intent);
        this.overridePendingTransition(0, 0);
    }
}
