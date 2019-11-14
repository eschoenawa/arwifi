package de.eschoenawa.wifiar.heatmap;

import android.graphics.Bitmap;
import android.graphics.Color;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.eschoenawa.wifiar.models.Polygon;
import io.github.jdiemke.triangulation.Vector2D;

/**
 * This class can generate a bitmap from a given two-dimensional double-array. The values in the array
 * represent a position along a given gradient and therefore each pixel gets a color on the gradient
 * representing it's value. This is meant to be used to generate heatmaps.
 *
 * @author Emil Schoenawa
 */
public class BitmapGenerator {
    private double[][] heatmap;
    private Polygon area;
    private ColorSelector colorSelector;
    private BitmapGeneratorCallback callback;
    private final long pixels;
    private long pixelsDone;

    /**
     * Creates a new BitmapGenerator
     *
     * @param heatmap       The heatmap for which the bitmap should be generated
     * @param area          The area as a {@link Polygon} in which the image should have color (pixels outside
     *                      this area will be transparent)
     * @param colorSelector The {@link ColorSelector} to use for value to color resolution
     */
    public BitmapGenerator(double[][] heatmap, Polygon area, ColorSelector colorSelector) {
        this.heatmap = heatmap;
        this.area = area;
        this.colorSelector = colorSelector;
        this.pixelsDone = 0;
        this.pixels = heatmap.length * heatmap[0].length;

    }

    private Bitmap drawHeatmap() {
        Bitmap result = Bitmap.createBitmap(heatmap.length, heatmap[0].length, Bitmap.Config.ARGB_8888);
        for (int x = 0; x < heatmap.length; x++) {
            for (int y = 0; y < heatmap[x].length; y++) {
                if (!Double.isNaN(heatmap[x][y]) && area.isPointInPolygon(new Vector2D(x, y))) {
                    result.setPixel(x, y, colorSelector.getColorForValue(heatmap[x][y]));
                } else {
                    result.setPixel(x, y, Color.TRANSPARENT);
                }
            }
            sendProgressUpdateToCallbackIfAvailable(heatmap[x].length);
        }
        return result;
    }

    public void drawHeatmapAsync(BitmapGeneratorCallback callback) {
        this.callback = callback;
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            Bitmap result = drawHeatmap();
            if (callback != null) {
                callback.onBitmapFinished(result);
            }
        });
    }

    private void sendProgressUpdateToCallbackIfAvailable(long pixelsDone) {
        synchronized (this) {
            this.pixelsDone += pixelsDone;
        }
        if (this.callback != null) {
            double percentageDone = (double) this.pixelsDone / this.pixels;
            this.callback.onBitmapProgress(percentageDone * 100);
        }
    }

    public interface BitmapGeneratorCallback {
        void onBitmapProgress(double percentage);

        void onBitmapFinished(Bitmap bitmap);
    }
}
