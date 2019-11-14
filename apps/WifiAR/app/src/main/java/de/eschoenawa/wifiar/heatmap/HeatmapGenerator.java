package de.eschoenawa.wifiar.heatmap;

import android.support.annotation.VisibleForTesting;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import de.eschoenawa.wifiar.common.Constants;
import de.eschoenawa.wifiar.models.WifiMeasurement;
import de.eschoenawa.wifiar.utils.concurrent.CompletionHelper;
import io.github.jdiemke.triangulation.DelaunayTriangulator;
import io.github.jdiemke.triangulation.NotEnoughPointsException;
import io.github.jdiemke.triangulation.Triangle2D;
import io.github.jdiemke.triangulation.Vector2D;

import static de.eschoenawa.wifiar.common.Constants.FUZZY_WIDTH;
import static de.eschoenawa.wifiar.common.Constants.INVALID_RSSI;
import static de.eschoenawa.wifiar.common.Constants.MAX_POWER;
import static de.eschoenawa.wifiar.common.Constants.MIN_POWER;

/**
 * This class generates a heatmap (two-dimensional-double-array) for specified WifiMeasurements. Missing
 * values will be interpolated between the known points. Four points will be generated one pixel outside
 * the desired heatmap-size at the edges of the map to allow filling the whole map with values. These
 * points will either take the value of the nearest point, assume a low value or assume a high value
 * depending on the {@link ExternalPointStrategy} set.
 *
 * @author Emil Schoenawa
 */
public class HeatmapGenerator {
    private int sizeX;
    private int sizeY;
    private List<WifiMeasurement> measurements;
    private ExternalPointStrategy externalPointStrategy;
    private double[][] heatmap;
    private Vector2D offset;
    private HeatmapGeneratorCallback callback;
    private ExecutorService executorService;
    private long pixels;
    private long pixelsDone;
    private Map<Triangle2D, WifiMeasurement[]> measurementsOfTriangleMap;

    private static final String TAG = "HEATMAPGEN";

    /**
     * Creates a new HeatmapGenerator.
     *
     * @param sizeX                 The size of the resulting heatmap in X-direction
     * @param sizeY                 The size of the resulting heatmap in Y-direction
     * @param zero                  The point of the coordinate-system of the measurement coordinates that should be
     *                              the coordinate origin for the resulting heatmap
     * @param externalPointStrategy The {@link ExternalPointStrategy} to apply to generate the edge
     *                              points of the heatmap
     * @param wifiMeasurements      The measurements made
     */
    public HeatmapGenerator(int sizeX, int sizeY, Vector2D zero, ExternalPointStrategy externalPointStrategy, List<WifiMeasurement> wifiMeasurements) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.pixels = sizeX * sizeY;
        this.pixelsDone = 0;
        this.externalPointStrategy = externalPointStrategy;
        this.measurements = wifiMeasurements;
        this.measurementsOfTriangleMap = new ConcurrentHashMap<>();
        offset = new Vector2D(-zero.x, -zero.y);
    }

    /**
     * This method generates the heatmap for the given measurement points. In order to interpolate
     * the value for each pixel there are multiple steps required. First a Delauney Triangulation
     * is performed to draw triangles between the measurement points. For this the java
     * implementation by Johannes Diemke of the incremental algorithm is used
     * (https://github.com/jdiemke/delaunay-triangulator). Afterwards the pixel values are
     * determined by using barycentric interpolation for each triangle
     *
     * @throws NotEnoughPointsException If not enough measurements were made
     */
    public void generateHeatmap() throws NotEnoughPointsException {

        // Offset measurement locations
        for (WifiMeasurement wifiMeasurement : measurements) {
            wifiMeasurement.x += offset.x;
            wifiMeasurement.y += offset.y;
        }

        // Generate measurements at edges of heatmap to allow extrapolation
        measurements.addAll(generateEdgePoints());

        // Fill pointSet with measurement points
        List<Vector2D> pointSet = new ArrayList<>(measurements);

        // Triangulate
        Log.d(TAG, "Starting triangulation...");
        DelaunayTriangulator triangulator = new DelaunayTriangulator(pointSet);
        triangulator.triangulate();
        Log.d(TAG, "Triangulation finished!");
        List<Triangle2D> triangles = triangulator.getTriangles();
        this.heatmap = new double[sizeX][sizeY];

        // Interpolate
        Log.d(TAG, "Starting interpolation...");
        interpolateHeatmapValues(triangles);
    }

    public void generateHeatmapAsync(HeatmapGeneratorCallback callback) {
        this.callback = callback;
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            try {
                generateHeatmap();
                if (HeatmapGenerator.this.callback != null) {
                    HeatmapGenerator.this.callback.onHeatmapGenerationFinished();
                }
            } catch (Exception e) {
                if (HeatmapGenerator.this.callback != null) {
                    HeatmapGenerator.this.callback.onHeatmapGenerationError(e);
                }
            }
        });
    }

    public double[][] getHeatmap() {
        return heatmap;
    }

    public Vector2D getOffset() {
        return offset;
    }

    private void interpolateHeatmapValues(List<Triangle2D> triangles) {
        executorService = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
        CompletionHelper completionHelper = new CompletionHelper();
        completionHelper.beforeSubmit();
        executorService.submit(() -> interpolateHeatmapValues(0, sizeX, 0, sizeY, triangles, completionHelper));
        try {
            completionHelper.awaitCompletion();
            executorService.shutdown();
            executorService.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted awaitTermination!");
        } finally {
            executorService.shutdownNow();
        }
        Log.d(TAG, "Interpolation of heatmap values completed.");
    }

    private void interpolateHeatmapValues(int startX, int endX, int startY, int endY, List<Triangle2D> triangles, CompletionHelper completionHelper) {
        if (shouldUseMoreThreads(startX, endX, startY, endY)) {
            // Delegate to more threads
            int deltaX = endX - startX;
            int deltaY = endY - startY;
            int divideX = startX + (deltaX / 2);
            int divideY = startY + (deltaY / 2);
            Log.d(TAG, "Delegating to 4 new threads: Pixelcount per thread is higher than threshold.");

            completionHelper.beforeSubmit(4);
            executorService.submit(() -> interpolateHeatmapValues(startX, divideX, startY, divideY, triangles, completionHelper));
            executorService.submit(() -> interpolateHeatmapValues(divideX, endX, startY, divideY, triangles, completionHelper));
            executorService.submit(() -> interpolateHeatmapValues(startX, divideX, divideY, endY, triangles, completionHelper));
            executorService.submit(() -> interpolateHeatmapValues(divideX, endX, divideY, endY, triangles, completionHelper));
        } else {
            Log.d(TAG, "Interpolating for x-range [" + startX + ", " + endX + "[ and for y-range [" + startY + ", " + endY + "[...");
            for (int x = startX; x < endX; x++) {
                for (int y = startY; y < endY; y++) {
                    this.heatmap[x][y] = interpolateValueAt(new Vector2D(x, y), triangles);
                }
                sendProgressUpdateToCallbackIfAvailable(endY - startY);
            }
            Log.d(TAG, "Interpolation for x-range [" + startX + ", " + endX + "[ and for y-range [" + startY + ", " + endY + "[ completed.");
        }
        completionHelper.taskCompleted();
    }

    private void sendProgressUpdateToCallbackIfAvailable(long pixelsDone) {
        synchronized (this) {
            this.pixelsDone += pixelsDone;
        }
        if (this.callback != null) {
            double percentageDone = (double) this.pixelsDone / this.pixels;
            this.callback.onHeatmapProgress(percentageDone * 100);
        }
    }

    private boolean shouldUseMoreThreads(int startX, int endX, int startY, int endY) {
        int deltaX = endX - startX;
        int deltaY = endY - startY;
        int divideX = startX + (deltaX / 2);
        int divideY = startY + (deltaY / 2);
        return deltaX * deltaY > Constants.MAX_VALUES_PER_THREAD && divideX > startX && divideX < endX && divideY > startY && divideY < endY;
    }

    /**
     * Determines the value at a point if the list of triangles contains a triangle that contains the point.
     * The value is determined by using {@link HeatmapGenerator#findClosestMeasurement(Vector2D)} for
     * all vertices of the triangle or by lookup in a map in which the measurements for already
     * looked up triangles are cached. Afterwards
     * {@link HeatmapGenerator#interpolateValueAt(Vector2D, WifiMeasurement, WifiMeasurement, WifiMeasurement)}
     * is used to determine the value to return. If no triangle contains the Point
     * {@link Constants#INVALID_RSSI} is returned.
     *
     * @param point     The point whose value should be determined
     * @param triangles The list of triangles
     * @return The interpolated value at the point
     */
    private double interpolateValueAt(Vector2D point, List<Triangle2D> triangles) {
        Triangle2D triangle = findTriangleContainingPoint(triangles, point);
        if (triangle == null) {
            Log.e(TAG, "Unable to find triangle containing point!");
            return INVALID_RSSI;
        }
        WifiMeasurement[] measurements = measurementsOfTriangleMap.get(triangle);
        // To avoid costly findClosestMeasurement()-method to run multiple times for the same
        // triangle use double-checked locking
        if (measurements == null || measurements.length != 3) {
            synchronized (triangle) {
                measurements = measurementsOfTriangleMap.get(triangle);
                if (measurements == null || measurements.length != 3) {
                    Log.d(TAG, "Measurement points for triangle '" + triangle.toString() + "' not cached yet, searching closest measurement to vertices.");
                    measurements = new WifiMeasurement[3];
                    measurements[0] = findClosestMeasurement(triangle.a);
                    measurements[1] = findClosestMeasurement(triangle.b);
                    measurements[2] = findClosestMeasurement(triangle.c);
                    measurementsOfTriangleMap.put(triangle, measurements);
                    Log.d(TAG, "Measurement points for triangle are now cached.");
                }
            }
        }
        return interpolateValueAt(point, measurements[0], measurements[1], measurements[2]);
    }

    /**
     * Determines the value at a point by using barycentric interpolation between the three measurements.
     * For this the measurements have to create a triangle that contains the point.
     *
     * @param point            The point whose value should be determined
     * @param wifiMeasurementA One of the Measurements
     * @param wifiMeasurementB One of the Measurements
     * @param wifiMeasurementC One of the Measurements
     * @return The value at the point; may be {@link Double#NaN} for extremely flat triangles
     */
    private double interpolateValueAt(Vector2D point, WifiMeasurement wifiMeasurementA, WifiMeasurement wifiMeasurementB, WifiMeasurement wifiMeasurementC) {
        double areaOfCompleteTriangle = getArea(new Triangle2D(wifiMeasurementA, wifiMeasurementB, wifiMeasurementC));
        double areaTriangleA = getArea(new Triangle2D(point, wifiMeasurementB, wifiMeasurementC));
        double areaTriangleB = getArea(new Triangle2D(point, wifiMeasurementA, wifiMeasurementC));
        double areaTriangleC = getArea(new Triangle2D(point, wifiMeasurementA, wifiMeasurementB));
        double weighedSum = (areaTriangleA * wifiMeasurementA.getPower()) + (areaTriangleB * wifiMeasurementB.getPower()) + (areaTriangleC * wifiMeasurementC.getPower());
        return weighedSum / areaOfCompleteTriangle;
    }

    /**
     * Calculates the area of a triangle by using Heron's formula
     *
     * @param triangle The triangle of which the area should be determined
     * @return The area of the triangle
     */
    @VisibleForTesting
    public double getArea(Triangle2D triangle) {
        double sideA = triangle.b.sub(triangle.c).mag();
        double sideB = triangle.c.sub(triangle.a).mag();
        double sideC = triangle.a.sub(triangle.b).mag();
        double s = (sideA + sideB + sideC) / 2;
        double result = Math.sqrt(s * (s - sideA) * (s - sideB) * (s - sideC));
        // If the triangle is small enough Double inaccuracy can cause NaN as a result. Assume no
        // area in that case.
        if (!Double.isNaN(result)) {
            return result;
        } else {
            Log.w(TAG, "Area for triangle '" + triangle.toString() + "' is NaN, 0 will be assumed instead.");
            return 0.0;
        }
    }

    /**
     * Finds the measurement from the measurement list that is closest to the given point. Be aware
     * that this is a very costly method (performance-wise) as vector-magnitudes are calculated.
     *
     * @param position The Point of which the closest measurement should be found
     * @return The closest measurement
     */
    private WifiMeasurement findClosestMeasurement(Vector2D position) {
        if (measurements.size() <= 0) {
            throw new IllegalStateException("No measurements available, cannot find nearest one!");
        }
        WifiMeasurement result = null;
        for (WifiMeasurement wifiMeasurement : measurements) {
            if (result == null || wifiMeasurement.sub(position).mag() < result.sub(position).mag()) {
                result = wifiMeasurement;
            }
        }
        return result;
    }

    /**
     * Generates Measurements at the edges slightly outside the target size to allow all values
     * inside the target size to be interpolated.
     *
     * @return A {@link List} with {@link WifiMeasurement}s slightly outside the target range
     */
    private List<WifiMeasurement> generateEdgePoints() {
        Vector2D p00 = new Vector2D(-1, -1);
        Vector2D p01 = new Vector2D(-1, sizeY);
        Vector2D p10 = new Vector2D(sizeX, -1);
        Vector2D p11 = new Vector2D(sizeX, sizeY);
        WifiMeasurement m00 = new WifiMeasurement(findClosestMeasurement(p00));
        m00.moveTo(p00);
        WifiMeasurement m01 = new WifiMeasurement(findClosestMeasurement(p01));
        m01.moveTo(p01);
        WifiMeasurement m10 = new WifiMeasurement(findClosestMeasurement(p10));
        m10.moveTo(p10);
        WifiMeasurement m11 = new WifiMeasurement(findClosestMeasurement(p11));
        m11.moveTo(p11);
        List<WifiMeasurement> result = new ArrayList<>();
        if (externalPointStrategy == ExternalPointStrategy.ASSUME_LOW) {
            m00.setPower(MIN_POWER);
            m01.setPower(MIN_POWER);
            m10.setPower(MIN_POWER);
            m11.setPower(MIN_POWER);
        } else if (externalPointStrategy == ExternalPointStrategy.ASSUME_HIGH) {
            m00.setPower(MAX_POWER);
            m01.setPower(MAX_POWER);
            m10.setPower(MAX_POWER);
            m11.setPower(MAX_POWER);
        }
        result.add(m00);
        result.add(m01);
        result.add(m10);
        result.add(m11);
        return result;
    }

    /**
     * Finds a triangle from the given List that contains the point. This method uses
     * {@link #fuzzyIsPointInTriangle(Vector2D, Triangle2D, double)} to determine if the point is
     * contained in the triangle it is currently checking.
     *
     * @param triangles The List of triangles
     * @param point     The point that should be in the returned triangle
     * @return The triangle that contains the point; if none is found {@code null} is returned
     */
    private Triangle2D findTriangleContainingPoint(List<Triangle2D> triangles, Vector2D point) {
        for (Triangle2D triangle : triangles) {
            if (fuzzyIsPointInTriangle(point, triangle, FUZZY_WIDTH)) {
                return triangle;
            }
        }
        return null;
    }

    /**
     * This method checks whether a point is in a triangle. Due to rounding errors with doubles
     * perfect mathematical solutions won't work for this. The method first makes a rough bounding
     * box check to see if the point could even be in the triangle. Afterwards the contains(Vector2D)
     * method of Triangle2D is used to filter points that are certainly inside the triangle. If that
     * method returns false eight more points are checked, first in a +, then in an X-shape with
     * the coordinates increased or decreased by fuzzyWidth. If a point of the plus or a point of
     * the X are in the triangle the point to check is considered to be inside the triangle as well,
     * otherwise not.
     *
     * @param point      The Point to check
     * @param triangle   The Triangle to check
     * @param fuzzyWidth The amount of inaccuracy allowed (the point can be this far outside the triangle to still be considered inside)
     * @return true if the point is in the triangle, false otherwise
     */
    private boolean fuzzyIsPointInTriangle(Vector2D point, Triangle2D triangle, double fuzzyWidth) {
        double minX = Math.min(triangle.a.x, Math.min(triangle.b.x, triangle.c.x)) - fuzzyWidth;
        double minY = Math.min(triangle.a.y, Math.min(triangle.b.y, triangle.c.y)) - fuzzyWidth;
        double maxX = Math.max(triangle.a.x, Math.max(triangle.b.x, triangle.c.x)) + fuzzyWidth;
        double maxY = Math.max(triangle.a.y, Math.max(triangle.b.y, triangle.c.y)) + fuzzyWidth;
        if (point.x < minX || point.y < minY || point.x > maxX || point.y > maxY) {
            return false;
        }
        if (triangle.contains(point)) {
            return true;
        }
        int hits = 0;
        // +-Shape
        Vector2D[] pointsToTest = new Vector2D[4];
        pointsToTest[0] = new Vector2D(point.x, point.y + fuzzyWidth);
        pointsToTest[1] = new Vector2D(point.x + fuzzyWidth, point.y);
        pointsToTest[2] = new Vector2D(point.x, point.y - fuzzyWidth);
        pointsToTest[3] = new Vector2D(point.x - fuzzyWidth, point.y);
        for (Vector2D pointToTest : pointsToTest) {
            if (triangle.contains(pointToTest)) {
                hits++;
            }
        }
        if (hits > 0) {
            return true;
        }
        // X-Shape
        pointsToTest = new Vector2D[4];
        pointsToTest[0] = new Vector2D(point.x + fuzzyWidth, point.y + fuzzyWidth);
        pointsToTest[1] = new Vector2D(point.x + fuzzyWidth, point.y - fuzzyWidth);
        pointsToTest[2] = new Vector2D(point.x - fuzzyWidth, point.y + fuzzyWidth);
        pointsToTest[3] = new Vector2D(point.x - fuzzyWidth, point.y - fuzzyWidth);
        for (Vector2D pointToTest : pointsToTest) {
            if (triangle.contains(pointToTest)) {
                hits++;
            }
        }
        return hits > 0;
    }

    /**
     * This Enum defines three different strategies for generating external (outside the bounds
     * of the target heatmap) Points. The Points either assume the lowest value possible, the value
     * of the nearest measurement or the highest value possible.
     */
    public enum ExternalPointStrategy {
        ASSUME_LOW, ASSUME_NEAREST, ASSUME_HIGH
    }

    public interface HeatmapGeneratorCallback {
        void onHeatmapProgress(double percentage);

        void onHeatmapGenerationFinished();

        void onHeatmapGenerationError(Exception exception);
    }
}
