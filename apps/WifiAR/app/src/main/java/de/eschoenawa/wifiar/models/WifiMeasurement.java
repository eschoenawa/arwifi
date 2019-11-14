package de.eschoenawa.wifiar.models;

import io.github.jdiemke.triangulation.Vector2D;

public class WifiMeasurement extends Vector2D {
    private double power;
    private double wavelength;

    public static final double RTT_DISTANCE_NONE = -1;

    public WifiMeasurement(WifiMeasurement wifiMeasurementToCopy) {
        this(wifiMeasurementToCopy.power, wifiMeasurementToCopy.wavelength, wifiMeasurementToCopy.x, wifiMeasurementToCopy.y);
    }

    public WifiMeasurement(double power, double wavelength, Vector2D position) {
        this(power, wavelength, position.x, position.y);
    }

    public WifiMeasurement(double power, double wavelength, double x, double y) {
        super(x, y);
        this.power = power;
        this.wavelength = wavelength;
    }

    public double getPower() {
        return power;
    }

    public double getWavelength() {
        return wavelength;
    }

    public void setPower(double power) {
        this.power = power;
    }

    public void moveTo(Vector2D position) {
        this.x = position.x;
        this.y = position.y;
    }
}
