package de.eschoenawa.wifiar.models;

import com.google.ar.sceneform.math.Vector3;

/**
 * This class allows the aggregation of rssi values to later retrieve the average value.
 *
 * @author Emil Schoenawa
 */
public class RssiAggregate {
    private double rssiSum;
    private int numberOfValues;
    private Vector3 worldLocation;

    public RssiAggregate() {
        rssiSum = 0;
        numberOfValues = 0;
    }

    public RssiAggregate(Vector3 worldLocation) {
        this();
        this.worldLocation = worldLocation;
    }

    public void addValue(double rssi) {
        rssiSum += rssi;
        numberOfValues++;
    }

    public double getAverage() {
        return rssiSum / (double) numberOfValues;
    }

    public Vector3 getWorldLocation() {
        return worldLocation;
    }

    public void setWorldLocation(Vector3 worldLocation) {
        this.worldLocation = worldLocation;
    }

    public int getNumberOfValues() {
        return numberOfValues;
    }
}
