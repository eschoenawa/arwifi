package de.eschoenawa.wifiar.controller;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import de.eschoenawa.wifiar.controller.callback.HeatmapGenerationControllerCallback;
import de.eschoenawa.wifiar.controller.manager.MaterialManager;
import de.eschoenawa.wifiar.heatmap.BitmapGenerator;
import de.eschoenawa.wifiar.heatmap.ColorSelector;
import de.eschoenawa.wifiar.heatmap.GradientColors;
import de.eschoenawa.wifiar.heatmap.HeatmapGenerator;
import de.eschoenawa.wifiar.models.Polygon;
import de.eschoenawa.wifiar.models.RssiAggregate;
import de.eschoenawa.wifiar.models.WifiMeasurement;
import de.eschoenawa.wifiar.utils.DialogHelper;
import de.eschoenawa.wifiar.utils.DrawingHelper;
import de.eschoenawa.wifiar.utils.Preferences;
import de.eschoenawa.wifiar.utils.ScaleHelper;
import de.eschoenawa.wifiar.utils.StateMachine;
import de.eschoenawa.wifiar.utils.UnitConverter;
import de.eschoenawa.wifiar.utils.Utils;
import de.eschoenawa.wifiar.utils.WifiDataCollector;
import io.github.jdiemke.triangulation.Vector2D;

import static de.eschoenawa.wifiar.common.Constants.MAX_MEASURE_DISTANCE;
import static de.eschoenawa.wifiar.common.Constants.MILLIS_BETWEEN_SCANS;
import static de.eschoenawa.wifiar.utils.StateMachine.State.AREA_COMPLETED;
import static de.eschoenawa.wifiar.utils.StateMachine.State.DISPLAY_HEATMAP;
import static de.eschoenawa.wifiar.utils.StateMachine.State.DRAW_AREA;
import static de.eschoenawa.wifiar.utils.StateMachine.State.FIND_PLANES;
import static de.eschoenawa.wifiar.utils.StateMachine.State.MEASURE;
import static de.eschoenawa.wifiar.utils.StateMachine.State.MEASURING;
import static de.eschoenawa.wifiar.utils.StateMachine.State.PLACE_AREA_ANCHOR;

public class HeatmapGenerationController implements Scene.OnUpdateListener, WifiDataCollector.ScanResultsAvailableListener, HeatmapGenerator.HeatmapGeneratorCallback, BitmapGenerator.BitmapGeneratorCallback {
    private static final String TAG = "HGC";
    private ArFragment arFragment;
    private HeatmapGenerationControllerCallback callback;
    private Context context;

    // Measurements
    private Stack<WifiMeasurement> measurements;

    // Nodes
    private Stack<Node> areaPoints;
    private Stack<Node> measurementPoints;
    private Node lastLineNode;

    // Anchors
    private Anchor targetAnchor;
    private Anchor areaAnchor;

    // Positions
    private Vector3 targetWorldPosition;
    private Vector3 areaAnchorWorldPosition;
    private Vector3 localBitmapZeroPosition;

    // Values
    private double sizeX;
    private double sizeY;

    // Materials
    private MaterialManager materialManager;

    // State
    private StateMachine stateMachine = new StateMachine();

    // HeatmapGenerator
    private HeatmapGenerator heatmapGenerator;

    // Models
    private Polygon area;
    private RssiAggregate rssiAggregate;

    // Heatmap Bitmap
    private Bitmap heatmap;

    // Flags
    private boolean permissionRequested;
    private boolean autoArea;
    private boolean autoAreaClosed;

    public HeatmapGenerationController(ArFragment arFragment, @NonNull HeatmapGenerationControllerCallback callback, Context context) {
        this.permissionRequested = false;
        this.arFragment = arFragment;
        this.callback = callback;
        this.context = context;
        this.materialManager = new MaterialManager(this.context);
        autoArea = Preferences.getInstance().isAreaAutoDefinitionEnabled();

        initArFragment();

        // Init Collections
        areaPoints = new Stack<>();
        measurements = new Stack<>();
        measurementPoints = new Stack<>();

        // Set state
        this.stateMachine.setState(FIND_PLANES);
        callback.onDenyUndo();
    }

    /**
     * This method initializes the {@code arFragment} field, enables this activity to
     * receive frame-updates and disables shadows on the ground for AR-Objects.
     */
    private void initArFragment() {
        arFragment.getArSceneView().getScene().setOnUpdateListener(this);
        arFragment.getArSceneView().getPlaneRenderer().setShadowReceiver(false);
    }

    @Override
    public void onUpdate(FrameTime frameTime) {
        try {
            if (!permissionRequested && callback != null) {
                callback.requestLocationPermission();
                permissionRequested = true;
            }
            if (!stateMachine.isAreaCompleted() || (autoArea && stateMachine.getState() == MEASURE && !autoAreaClosed)) {
                handleTargetAnchorUpdate();
            }
            updateAreaAnchorWorldPosition();
            updateLineBetweenLastAreaPointAndTarget();
            if (arFragment.getArSceneView().getSession().getAllTrackables(Plane.class).size() > 0 && stateMachine.getState() == FIND_PLANES) {
                if (!autoArea) {
                    this.stateMachine.setState(PLACE_AREA_ANCHOR);
                } else {
                    this.stateMachine.setState(MEASURE);
                }
                if (callback != null) {
                    callback.onPlaneDetected();
                    if (autoArea) {
                        callback.onEnterMeasureState();
                    }
                }
            }

            // Required because Scene.setOnUpdateListener() replaces fragment as listener
            arFragment.onUpdate(frameTime);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(getClass().getSimpleName(), "Exception in onUpdate!", e);
        }
    }

    /**
     * Generates the line between the last placed area point (if available) and
     * the currently targeted position
     */
    private void updateLineBetweenLastAreaPointAndTarget() {
        if (targetAnchor != null && (stateMachine.getState() == DRAW_AREA) || (autoArea && stateMachine.getState() == MEASURE && !autoAreaClosed)) {
            AnchorNode anchorNode = new AnchorNode(targetAnchor);
            anchorNode.setParent(arFragment.getArSceneView().getScene());
            Node n = new Node();
            n.setParent(anchorNode);
            n.setWorldPosition(targetWorldPosition);
            DrawingHelper.drawLineBetweenNodes(n, areaPoints.peek(), materialManager.getAreaLineMaterial());
        }
    }

    /**
     * Ensures the AreaAnchorWorldPosition is accurate.
     */
    private void updateAreaAnchorWorldPosition() {
        if (areaAnchor != null && areaPoints.size() > 0) {
            areaAnchorWorldPosition = areaPoints.firstElement().getWorldPosition();
        }
    }

    private void handleTargetAnchorUpdate() {
        if (targetAnchor != null) {
            targetAnchor.detach();
        }
        targetAnchor = autoArea ? createAnchorAtCameraPosition() : createAnchorAtCurrentTarget();
        Node n = DrawingHelper.attachPillarToAnchorIfPossible(targetAnchor, materialManager.getTargetIndicatorMaterial(), arFragment.getArSceneView().getScene());
        if (n != null) {
            targetWorldPosition = n.getWorldPosition();
            // Snap to start of area
            if (currentlyTargetingAreaStart()) {
                n.setWorldPosition(areaAnchorWorldPosition);
                targetWorldPosition = areaAnchorWorldPosition;
            }
        }
    }

    /**
     * Creates an area-indicator at the position the camera is currently pointing at.
     * That indicator is stored in a stack so its coordinates can be requested later
     * to allow undoing the placement of area indicators.
     */
    private void createAreaPointAtCurrentTarget() {
        if (areaAnchor == null) {
            areaAnchor = autoArea ? createAnchorAtCameraPosition() : createAnchorAtCurrentTarget();
            Node n = DrawingHelper.attachPillarToAnchorIfPossible(areaAnchor, materialManager.getAreaIndicatorMaterial(), arFragment.getArSceneView().getScene());
            if (n != null) {
                areaPoints = new Stack<>();
                areaPoints.add(n);
                areaAnchorWorldPosition = n.getWorldPosition();
            }
            if (callback != null) {
                callback.onAllowUndo();
            }
            stateMachine.setState(autoArea ? MEASURE : DRAW_AREA);
        } else {
            Node n = DrawingHelper.attachPillarToAnchorIfPossible(areaAnchor, materialManager.getAreaIndicatorMaterial(), targetWorldPosition, arFragment.getArSceneView().getScene());
            if (n != null) {
                n = DrawingHelper.correctToSameHeight(n, areaPoints.firstElement());
                Node lineNode = new Node();
                lineNode.setParent(n);
                DrawingHelper.drawLineBetweenNodes(lineNode, areaPoints.peek(), materialManager.getAreaLineMaterial());
                areaPoints.add(n);
            }
        }
    }

    /**
     * Creates an Anchor at the position the camera is currently pointing at.
     *
     * @return The created Anchor
     */
    private Anchor createAnchorAtCurrentTarget() {
        if (callback != null) {
            float[] screenSize = Utils.getScreenSize(callback.getActivity());
            List<HitResult> hitResults = arFragment.getArSceneView().getArFrame().hitTest(screenSize[0] / 2, screenSize[1] / 2);
            return (hitResults.size() > 0) ? hitResults.get(0).createAnchor() : null;
        }
        return null;
    }

    private Anchor createAnchorAtCameraPosition() {

        if (callback != null) {
            Vector3 camPosition = arFragment.getArSceneView().getScene().getCamera().getWorldPosition();
            if (areaAnchor == null) {
                float[] screenSize = Utils.getScreenSize(callback.getActivity());
                List<HitResult> hitResults = arFragment.getArSceneView().getArFrame().hitTest(screenSize[0] / 2, screenSize[1] / 2);
                HitResult result = hitResults.get(0);
                if (result == null) {
                    return null;
                }
                camPosition.y = result.getHitPose().ty();
            } else {
                camPosition.y = areaAnchorWorldPosition.y;
            }
            return arFragment.getArSceneView().getSession().createAnchor(Pose.makeTranslation(camPosition.x, camPosition.y, camPosition.z));
        }
        return null;
    }

    private boolean currentlyTargetingAreaStart() {
        return Utils.positionsCloseEnough(areaAnchorWorldPosition, targetWorldPosition);
    }

    private void finishArea() {
        if (areaPoints.size() < 3) {
            Toast.makeText(context, "An area requires at least 3 points!", Toast.LENGTH_LONG).show();
        } else if (stateMachine.isAreaCompleted() && !autoArea) {
            stateMachine.setState(MEASURE);
            if (callback != null) {
                callback.onEnterMeasureState();
            }
        } else {
            stateMachine.setState(autoArea ? MEASURE : AREA_COMPLETED);
            autoAreaClosed = true;
            targetAnchor.detach();
            targetAnchor = null;
            lastLineNode = new Node();
            lastLineNode.setParent(areaPoints.peek());
            DrawingHelper.drawLineBetweenNodes(lastLineNode, areaPoints.firstElement(), materialManager.getAreaLineMaterial());
            if (callback != null && !autoArea) {
                callback.onAreaCompleted();
            }
        }
    }

    /**
     * This method removes the last point placed. If the area is already finished
     * it undoes the finishing of the area but won't remove the last point placed.
     */
    public void performUndo() {
        if (!autoArea) {
            switch (stateMachine.getState()) {
                case DRAW_AREA:
                    Node nodeToRemove = areaPoints.pop();
                    nodeToRemove.setParent(null);
                    if (areaPoints.isEmpty()) {
                        areaAnchor.detach();
                        areaAnchor = null;
                        areaAnchorWorldPosition = null;
                        stateMachine.setState(PLACE_AREA_ANCHOR);
                        if (callback != null) {
                            callback.onDenyUndo();
                        }
                    } else if (areaPoints.size() < 3) {
                        if (callback != null) {
                            callback.onDrawAreaState(false);
                        }
                    }
                    break;
                case MEASURE:
                    if (!measurementPoints.isEmpty()) {
                        Node nodeToDelete = measurementPoints.pop();
                        nodeToDelete.setParent(null);
                        measurements.pop();
                        if (measurements.size() <= 2 && callback != null) {
                            callback.onDenyGeneration();
                        }
                        break;
                    }
                    // No break to undo finishing area as undoing 'enter measure mode' alone might be confusing
                case AREA_COMPLETED:
                    lastLineNode.setParent(null);
                    lastLineNode = null;
                    stateMachine.setState(DRAW_AREA);
                    if (callback != null) {
                        callback.onDrawAreaState(true);
                    }
            }
        } else {
            if (measurementPoints.size() > areaPoints.size()) {
                Node nodeToDelete = measurementPoints.pop();
                nodeToDelete.setParent(null);
                measurements.pop();
            } else if (!areaPoints.isEmpty() && areaPoints.size() == measurementPoints.size()) {
                // Remove measurement
                Node nodeToDelete = measurementPoints.pop();
                nodeToDelete.setParent(null);
                measurements.pop();
                // Remove area point
                Node nodeToRemove = areaPoints.pop();
                nodeToRemove.setParent(null);
                autoAreaClosed = false;
                if (callback != null) {
                    callback.onDenyGeneration();
                }
                if (areaPoints.isEmpty()) {
                    areaAnchor.detach();
                    areaAnchor = null;
                    areaAnchorWorldPosition = null;
                    if (callback != null) {
                        callback.onDenyUndo();
                    }
                }

            }
        }
    }

    public void addAreaPoint() {
        if (currentlyTargetingAreaStart()) {
            finishArea();
        } else {
            createAreaPointAtCurrentTarget();
            if (callback != null) {
                callback.onDrawAreaState(areaPoints.size() > 2);
            }
        }
    }

    public void measureAtCurrentPosition() {
        if (autoArea && !autoAreaClosed) {
            addAreaPoint();
            if (autoAreaClosed) {
                // Don't insert measurement at start as there already is one.
                if (callback != null) {
                    callback.onAllowGeneration();
                }
                return;
            }
        }
        WifiDataCollector wifiData = WifiDataCollector.getInstance();
        Vector3 camPosition = arFragment.getArSceneView().getScene().getCamera().getWorldPosition();
        camPosition.y = areaAnchorWorldPosition.y;
        if (stateMachine.getState() == MEASURE && callback != null) {
            boolean waitForScanResults = Preferences.getInstance().shouldWaitForNewWifiScan();
            stateMachine.setState(MEASURING);
            rssiAggregate = new RssiAggregate(camPosition);
            rssiAggregate.addValue(wifiData.getCurrentRssiInWatts());
            callback.onMeasurementStart();
            if (waitForScanResults) {
                wifiData.setScanResultsAvailableListener(this);
                wifiData.startReceivingScanResults(callback.getActivity());
            }
            Handler handler = new Handler();
            handler.postDelayed(waitForScanResults ? wifiData::startScan : createTimeBasedRssiMeasurementTrigger(), MILLIS_BETWEEN_SCANS);
            measurementPoints.add(DrawingHelper.attachPillarToAnchorIfPossible(areaAnchor, materialManager.getMeasurementIndicatorMaterial(), camPosition, arFragment.getArSceneView().getScene()));
        }
    }

    public void startHeatmapGeneration() {
        this.heatmapGenerator = createHeatmapGenerator();
        this.area = createArea(heatmapGenerator.getOffset());
        heatmapGenerator.generateHeatmapAsync(this);
        if (callback != null) {
            callback.onGenerationStarted();
        }

    }

    public void tipTextClicked() {
        if (this.stateMachine.getState() == DISPLAY_HEATMAP && heatmapGenerator != null && heatmapGenerator.getHeatmap() != null && callback != null) {
            double measuredRssi = WifiDataCollector.getInstance().getCurrentRssiInDbm();
            Vector3 camPosition = arFragment.getArSceneView().getScene().getCamera().getWorldPosition();
            Vector2D heatmapCoordinates = worldCoordinatesToHeatmapCoordinates(camPosition);
            int x = (int) heatmapCoordinates.x;
            int y = (int) heatmapCoordinates.y;
            double[][] heatmap = heatmapGenerator.getHeatmap();
            if (x < heatmap.length && y < heatmap[0].length) {
                double heatmapRssi = UnitConverter.wattsToDbm(heatmap[x][y]);
                callback.uiOperation(() -> Toast.makeText(context, "Measured: " + measuredRssi + "dBm\nHeatmap: " + heatmapRssi + "dBm", Toast.LENGTH_LONG).show());
            } else {
                callback.uiOperation(() -> Toast.makeText(context, "Außerhalb der Heatmap!", Toast.LENGTH_SHORT).show());
            }
        }
    }

    public Bitmap getHeatmap() {
        return heatmap;
    }

    public void refreshAreaDesignationMode() {
        autoArea = Preferences.getInstance().isAreaAutoDefinitionEnabled();
    }

    private Runnable createTimeBasedRssiMeasurementTrigger() {
        return () -> this.performRssiMeasurement(false);
    }

    private void performRssiMeasurement(boolean waitForNewScanResults) {
        Log.d(TAG, "ScanResults available!");
        if (stateMachine.getState() == MEASURING) {
            WifiDataCollector wifiData = WifiDataCollector.getInstance();
            Vector3 camPosition = arFragment.getArSceneView().getScene().getCamera().getWorldPosition();
            camPosition.y = areaAnchorWorldPosition.y;
            Vector3 startMeasurementPosition = rssiAggregate.getWorldLocation();
            Vector2D vectorBetweenStartMeasurementPositionAndCameraPositionOnPlane = new Vector2D(camPosition.x - startMeasurementPosition.x, camPosition.z - startMeasurementPosition.z);
            if (vectorBetweenStartMeasurementPositionAndCameraPositionOnPlane.mag() > MAX_MEASURE_DISTANCE) {
                DialogHelper.showTooMuchMovementDialog(context);
                rssiAggregate = null;
                stateMachine.setState(MEASURE);
                if (callback != null) {
                    callback.onEnterMeasureState();
                    if (waitForNewScanResults) {
                        wifiData.stopReceivingScanResults(callback.getActivity());
                        wifiData.setScanResultsAvailableListener(null);
                    }
                }
                Node n = measurementPoints.pop();
                n.setParent(null);
                return;
            }
            rssiAggregate.addValue(wifiData.getCurrentRssiInWatts());
            if (rssiAggregate.getNumberOfValues() >= Preferences.getInstance().getNumberOfMeasurements()) {
                double rssi = rssiAggregate.getAverage();
                Toast.makeText(context, "RSSI: " + UnitConverter.wattsToDbm(rssi) + "dBm", Toast.LENGTH_SHORT).show();
                Vector2D vectorBetweenAreaAnchorAndCameraPositionOnPlane = new Vector2D(camPosition.x - areaAnchorWorldPosition.x, camPosition.z - areaAnchorWorldPosition.z);
                WifiMeasurement measurement = new WifiMeasurement(wifiData.getCurrentRssiInWatts(), wifiData.getCurrentFrequency(), vectorBetweenAreaAnchorAndCameraPositionOnPlane);
                measurements.add(measurement);
                rssiAggregate = null;
                stateMachine.setState(MEASURE);
                if (callback != null) {
                    callback.onEnterMeasureState();
                    if ((measurements.size() > 2 && !autoArea) || (autoArea && autoAreaClosed)) {
                        callback.onAllowGeneration();
                    }
                    if (waitForNewScanResults) {
                        wifiData.stopReceivingScanResults(callback.getActivity());
                        wifiData.setScanResultsAvailableListener(null);
                    }
                }
            } else {
                Handler handler = new Handler();
                handler.postDelayed(waitForNewScanResults ? wifiData::startScan : createTimeBasedRssiMeasurementTrigger(), MILLIS_BETWEEN_SCANS);
            }
        }
    }

    private Vector2D worldCoordinatesToHeatmapCoordinates(Vector3 position) {
        if (heatmapGenerator == null || heatmapGenerator.getHeatmap() == null) {
            throw new IllegalStateException("Unable to convert to heatmap coordinates: Heatmap not created yet!");
        }
        Vector2D offset = heatmapGenerator.getOffset();
        return new Vector2D(ScaleHelper.metersToPixels(position.x) + offset.x, ScaleHelper.metersToPixels(position.z) + offset.y);
    }

    private Polygon createArea(Vector2D offset) {
        List<Vector2D> points = new ArrayList<>();
        for (Node node : areaPoints) {
            Vector3 nodeCoords = node.getWorldPosition();
            Vector2D newPoint = new Vector2D(ScaleHelper.metersToPixels(nodeCoords.x) + offset.x, ScaleHelper.metersToPixels(nodeCoords.z) + offset.y);
            points.add(newPoint);
        }
        return new Polygon(points);
    }

    private HeatmapGenerator createHeatmapGenerator() {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;
        float height = areaAnchorWorldPosition.y;
        for (int i = 0; i < measurementPoints.size(); i++) {
            Node node = measurementPoints.get(i);
            minX = Math.min(minX, node.getWorldPosition().x);
            minY = Math.min(minY, node.getWorldPosition().z);
            maxX = Math.max(maxX, node.getWorldPosition().x);
            maxY = Math.max(maxY, node.getWorldPosition().z);
            // Update measurement position to ensure the measurement is placed where ARCore thinks the measurement took place. Also change coordinates from meters to pixels
            measurements.get(i).x = ScaleHelper.metersToPixels(node.getWorldPosition().x);
            measurements.get(i).y = ScaleHelper.metersToPixels(node.getWorldPosition().z);

        }
        for (Node node : areaPoints) {
            minX = Math.min(minX, node.getWorldPosition().x);
            minY = Math.min(minY, node.getWorldPosition().z);
            maxX = Math.max(maxX, node.getWorldPosition().x);
            maxY = Math.max(maxY, node.getWorldPosition().z);
        }
        Log.d(TAG, "minX = " + minX);
        Log.d(TAG, "minY = " + minY);
        Log.d(TAG, "maxX = " + maxX);
        Log.d(TAG, "maxY = " + maxY);
        localBitmapZeroPosition = areaPoints.firstElement().worldToLocalPoint(new Vector3((float) minX, height, (float) minY));
        if (Preferences.getInstance().shouldDrawDebugLines()) {
            drawHelperMarkers((float) minX, (float) maxX, (float) minY, (float) maxY, height);
        }
        Vector2D zero = new Vector2D(ScaleHelper.metersToPixels(minX), ScaleHelper.metersToPixels(minY));
        sizeX = Math.abs(minX - maxX);
        sizeY = Math.abs(minY - maxY);
        Log.d(TAG, "sizeX = " + sizeX);
        Log.d(TAG, "sizeY = " + sizeY);
        return new HeatmapGenerator(ScaleHelper.metersToPixels(sizeX), ScaleHelper.metersToPixels(sizeY), zero, HeatmapGenerator.ExternalPointStrategy.ASSUME_NEAREST, measurements);
    }

    /**
     * This method helps for debugging in 3d-space. It draws 4 markers to indicate the area
     * designated by the min-max values. The marker at the min/min coordinate is red
     * and all other coordinates are green. The Y-Axis will get a yellow line while the X-Axis
     * gets a blue line. Note that this method only works if an area is already defined.
     *
     * @param minX   The smallest X
     * @param maxX   The highest X
     * @param minY   The smallest Y
     * @param maxY   The highest Y
     * @param height The height on which the rectangle should be drawn
     */
    private void drawHelperMarkers(float minX, float maxX, float minY, float maxY, float height) {
        DrawingHelper.attachPillarToAnchorIfPossible(areaAnchor, materialManager.getTargetIndicatorMaterial(), new Vector3(minX, height, minY), arFragment.getArSceneView().getScene());
        DrawingHelper.attachPillarToAnchorIfPossible(areaAnchor, materialManager.getMeasurementIndicatorMaterial(), new Vector3(minX, height, maxY), arFragment.getArSceneView().getScene());
        DrawingHelper.attachPillarToAnchorIfPossible(areaAnchor, materialManager.getMeasurementIndicatorMaterial(), new Vector3(maxX, height, minY), arFragment.getArSceneView().getScene());
        DrawingHelper.attachPillarToAnchorIfPossible(areaAnchor, materialManager.getMeasurementIndicatorMaterial(), new Vector3(maxX, height, maxY), arFragment.getArSceneView().getScene());
        Node minMin1 = new Node();
        minMin1.setParent(areaPoints.firstElement());
        minMin1.setWorldPosition(new Vector3(minX, height, minY));
        Node minMin2 = new Node();
        minMin2.setParent(areaPoints.firstElement());
        minMin2.setWorldPosition(new Vector3(minX, height, minY));
        DrawingHelper.drawLineAtNodeToPoint(minMin1, new Vector3(maxX, height, minY), materialManager.getAreaIndicatorMaterial());
        DrawingHelper.drawLineAtNodeToPoint(minMin2, new Vector3(minX, height, maxY), materialManager.getAreaLineMaterial());
    }

    private void notifyOfProgress(double percentage) {
        if (callback != null) {
            callback.setProgress(percentage);
        }
    }

    @Override
    public void onScanResultsAvailable() {
        Log.d(TAG, "ScanResults available!");
        performRssiMeasurement(true);
    }

    @Override
    public void onHeatmapProgress(double percentage) {
        notifyOfProgress(percentage);
    }

    @Override
    public void onHeatmapGenerationFinished() {
        if (callback != null) {
            callback.onRenderStarted();
        }
        double[][] heatmap = heatmapGenerator.getHeatmap();
        if (heatmap != null) {
            Preferences prefs = Preferences.getInstance();
            ColorSelector colorSelector = new ColorSelector(prefs.getColorSelectionMode(), new GradientColors(prefs.getHeatmapColors()), ColorSelector.findMinAndMaxValuesInHeatmap(area, heatmap));
            BitmapGenerator bitmapGenerator = new BitmapGenerator(heatmap, area, colorSelector);
            bitmapGenerator.drawHeatmapAsync(this);
        }
    }

    @Override
    public void onHeatmapGenerationError(Exception exception) {
        Log.e(TAG, "Failed to generate heatmap!", exception);
        Toast.makeText(context, "Failed to generate heatmap!", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onBitmapProgress(double percentage) {
        notifyOfProgress(percentage);
    }

    @Override
    public void onBitmapFinished(Bitmap bitmap) {
        Log.d(TAG, "Heatmap image generated!");
        if (callback != null) {
            callback.onRenderFinished();
            callback.uiOperation(() -> {
                Node node = new Node();
                node.setParent(areaPoints.firstElement());
                ViewRenderable heatmapRenderable = callback.getHeatmapRenderable();
                node.setRenderable(heatmapRenderable);
                callback.getHeatmapImageView().setImageBitmap(bitmap);

                correctHeatmapWorldPosition(node);
            });
            this.stateMachine.setState(DISPLAY_HEATMAP);
            this.heatmap = bitmap;
        }
    }

    private void correctHeatmapWorldPosition(Node node) {
        // correct rotation for heatmap
        node.setWorldRotation(new Quaternion(0, 0, 0, 1));
        node.setWorldRotation(Quaternion.multiply(Quaternion.axisAngle(Vector3.up(), 180), Quaternion.axisAngle(Vector3.right(), 90)));
        // correct worldposition
        node.setLocalPosition(localBitmapZeroPosition);
        // correct offset for heatmap (ViewRenderer has anchor point in the middle)
        node.setWorldPosition(Vector3.add(node.getWorldPosition(), new Vector3((float) (sizeX / 2), 0, (float) sizeY)));
    }
}
