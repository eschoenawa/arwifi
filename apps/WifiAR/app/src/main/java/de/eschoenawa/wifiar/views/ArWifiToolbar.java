package de.eschoenawa.wifiar.views;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import de.eschoenawa.wifiar.R;

/**
 * This compound view is a toolbar (Button Bar) for user input for measuring wifi. Various state
 * callback methods allow the activity using this toolbar to set certain states without having to
 * manually set view visibilities, icons and enabled-states.
 *
 * @author Emil Schoenawa
 */
public class ArWifiToolbar extends LinearLayout {

    // Views
    private ButtonBarIconButton btnSettings;
    private ButtonBarIconButton btnUndo;
    private ButtonBarIconButton btnAddPoint;
    private ButtonBarIconButton btnMeasure;
    private ButtonBarIconButton btnFinish;
    private ButtonBarIconButton btnRestart;
    private ButtonBarIconButton btnCreateImage;
    private ProgressBar spinner;

    public ArWifiToolbar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.ar_wifi_toolbar, this);
        btnSettings = findViewById(R.id.btnSettings);
        btnUndo = findViewById(R.id.btnUndo);
        btnAddPoint = findViewById(R.id.btnAddPointAndFinishArea);
        btnMeasure = findViewById(R.id.btnMeasure);
        btnFinish = findViewById(R.id.btnCreateHeatmap);
        btnRestart = findViewById(R.id.btnRestart);
        btnCreateImage = findViewById(R.id.btnCreateImage);
        spinner = findViewById(R.id.toolbarLoading);

    }

    public void onAllowUndo() {
        btnUndo.setEnabled(true);
    }

    public void onDenyUndo() {
        btnUndo.setEnabled(false);
    }

    public void onDrawAreaState() {
        btnMeasure.setVisibility(GONE);
        btnFinish.setVisibility(GONE);
        btnAddPoint.setVisibility(VISIBLE);
        btnAddPoint.setImage(getContext().getDrawable(R.drawable.ic_add_location_white));
    }

    public void onAreaCompleted() {
        btnAddPoint.setImage(getContext().getDrawable(R.drawable.ic_done_white));
    }

    public void onEnterMeasureState() {
        btnAddPoint.setVisibility(GONE);
        btnMeasure.setVisibility(VISIBLE);
        btnFinish.setVisibility(VISIBLE);
        btnFinish.setEnabled(false);
        spinner.setVisibility(GONE);
    }

    public void onMeasurementStart() {
        spinner.setVisibility(VISIBLE);
    }

    public void onAllowGeneration() {
        btnFinish.setEnabled(true);
    }

    public void onDenyGeneration() {
        btnFinish.setEnabled(false);
    }

    public void onGeneration() {
        btnSettings.setVisibility(GONE);
        btnUndo.setVisibility(GONE);
        btnMeasure.setVisibility(GONE);
        btnFinish.setVisibility(GONE);
        btnRestart.setVisibility(VISIBLE);
        btnCreateImage.setVisibility(VISIBLE);
        spinner.setVisibility(VISIBLE);
    }

    public void onHeatmapDisplay() {
        spinner.setVisibility(GONE);
    }
}
