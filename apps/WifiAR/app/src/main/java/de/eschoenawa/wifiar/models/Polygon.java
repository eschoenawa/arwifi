package de.eschoenawa.wifiar.models;

import java.util.List;

import io.github.jdiemke.triangulation.Vector2D;

public class Polygon {
    private List<Vector2D> points;

    public Polygon(List<Vector2D> points) {
        this.points = points;
    }

    public boolean isPointInPolygon(Vector2D point) {
        int i;
        int j;
        boolean result = false;
        for (i = 0, j = points.size() - 1; i < points.size(); j = i++) {
            Vector2D pointI = points.get(i);
            Vector2D pointJ = points.get(j);
            if ((pointI.y > point.y) != (pointJ.y > point.y) &&
                    (point.x < (pointJ.x - pointI.x) * (point.y - pointI.y) / (pointJ.y - pointI.y) + pointI.x)) {
                result = !result;
            }
        }
        return result;
    }
}
