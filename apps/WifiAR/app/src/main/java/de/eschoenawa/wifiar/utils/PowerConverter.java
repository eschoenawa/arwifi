package de.eschoenawa.wifiar.utils;

public class PowerConverter {
    public static float dbmToWatts(float decibel) {
        return (float) Math.pow(10, (decibel - 30) / 10);
    }

    public static float wattsToDbm(float watts) {
        return (float) (30 + (10 * Math.log10(watts)));
    }
}
