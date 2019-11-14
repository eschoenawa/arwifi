package de.eschoenawa.wifiar.controller.callback;

import android.app.Activity;
import android.widget.ImageView;

import com.google.ar.sceneform.rendering.ViewRenderable;

public interface HeatmapGenerationControllerCallback {
    void setProgress(double percentage);

    void onPlaneDetected();

    void onAllowUndo();

    void onDenyUndo();

    void onDrawAreaState(boolean areaCanBeFinished);

    void onAreaCompleted();

    void onEnterMeasureState();

    void onMeasurementStart();

    void onAllowGeneration();

    void onDenyGeneration();

    void onGenerationStarted();

    void onRenderStarted();

    void onRenderFinished();

    Activity getActivity();

    ViewRenderable getHeatmapRenderable();

    ImageView getHeatmapImageView();

    void uiOperation(Runnable runnable);

    void requestLocationPermission();
}
