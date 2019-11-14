package de.eschoenawa.wifiar;

import org.junit.Test;

import de.eschoenawa.wifiar.heatmap.HeatmapGenerator;
import io.github.jdiemke.triangulation.Triangle2D;
import io.github.jdiemke.triangulation.Vector2D;

import static org.junit.Assert.assertEquals;

public class HeatmapGeneratorTest {
    @Test
    public void testTriangleAreaCalculation() {
        HeatmapGenerator heatmapGenerator = new HeatmapGenerator(100, 100, new Vector2D(2, 2), HeatmapGenerator.ExternalPointStrategy.ASSUME_NEAREST, null);
        double area = heatmapGenerator.getArea(new Triangle2D(new Vector2D(0, 4), new Vector2D(3, 0), new Vector2D(0, 0)));
        assertEquals(6, area, 0.00000000001);
    }
}
