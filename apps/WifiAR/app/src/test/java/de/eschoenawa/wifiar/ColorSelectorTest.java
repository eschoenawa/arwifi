package de.eschoenawa.wifiar;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import de.eschoenawa.wifiar.heatmap.ColorSelector;
import de.eschoenawa.wifiar.models.Polygon;
import io.github.jdiemke.triangulation.Vector2D;

import static org.junit.Assert.assertEquals;

public class ColorSelectorTest {
    @Test
    public void testFindMinMaxHeatmapValues() {
        double[][] heatmap = new double[2][2];
        heatmap[0][0] = 1;
        heatmap[0][1] = 2;
        heatmap[1][0] = 3;
        heatmap[1][1] = 4;

        // Define area (exclude P(0|0)
        List<Vector2D> area = new ArrayList<>();
        area.add(new Vector2D(-0.1, 1.1));
        area.add(new Vector2D(1.1, -0.1));
        area.add(new Vector2D(1.1, 1.1));
        Polygon testPolygon = new Polygon(area);

        double[] minMaxValues = ColorSelector.findMinAndMaxValuesInHeatmap(testPolygon, heatmap);
        assertEquals(2, minMaxValues[0], 0);
        assertEquals(4, minMaxValues[1], 0);
    }

    @Test
    public void testGetMode() {
        String modeBounds = "BOUNDS";
        String modeWifiBars = "WIFI_BARS";
        assertEquals(ColorSelector.Mode.BOUNDS, ColorSelector.getMode(modeBounds));
        assertEquals(ColorSelector.Mode.WIFI_BARS, ColorSelector.getMode(modeWifiBars));
    }
}
