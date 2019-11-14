package de.eschoenawa.wifiar;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import de.eschoenawa.wifiar.models.Polygon;
import de.eschoenawa.wifiar.models.RssiAggregate;
import io.github.jdiemke.triangulation.Vector2D;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class ModelsTest {
    @Test
    public void testIsPointInPolygon() {
        List<Vector2D> points = new ArrayList<>();
        points.add(new Vector2D(0,0));
        points.add(new Vector2D(0,1));
        points.add(new Vector2D(1,1));
        points.add(new Vector2D(1,0));

        Polygon polygon = new Polygon(points);
        assertTrue(polygon.isPointInPolygon(new Vector2D(0.5, 0.5)));
        assertFalse(polygon.isPointInPolygon(new Vector2D(1.5, 1.5)));

        // The method used for the point-in-polygon-test (raycast) while being faster than other methods
        // causes inaccuracies on handling Points on the edge. For the use-case of creating a
        // Wifi-Heatmap however this can be ignored as a few pixels colored on the line while not
        // being colored in other places on the line won't have any noticeable impact. Because of
        // this points on the edge will not be tested here.
    }

    @Test
    public void testRssiAverageCalculation() {
        RssiAggregate rssiAggregate = new RssiAggregate();
        rssiAggregate.addValue(1);
        rssiAggregate.addValue(2);
        rssiAggregate.addValue(3);
        assertEquals(2, rssiAggregate.getAverage(), 0);
        rssiAggregate.addValue(4);
        assertEquals(2.5, rssiAggregate.getAverage(), 0);
    }
}
