package de.eschoenawa.wifiar.utils;

import static de.eschoenawa.wifiar.common.Constants.MIN_POWER;
import static de.eschoenawa.wifiar.common.Constants.MIN_POWER_DBM;

/**
 * This utility-class allows the conversion of units with a collection of helper methods.
 *
 * @author Emil Schoenawa
 */
public class UnitConverter {
    public static double dbmToWatts(double decibel) {
        if (decibel <= MIN_POWER_DBM) {
            return 0;
        }
        return Math.pow(10, (decibel - 30) / 10);
    }

    public static double wattsToDbm(double watts) {
        if (watts == MIN_POWER) {
            return MIN_POWER_DBM;
        }
        return 30 + (10 * Math.log10(watts));
    }

    public static double megaToUnit(double megas) {
        return megas / 1000000.0;
    }
}
