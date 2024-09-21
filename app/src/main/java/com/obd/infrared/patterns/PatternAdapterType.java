package com.obd.infrared.patterns;

import android.os.Build;

import androidx.annotation.Nullable;
import com.obd.infrared.transmit.TransmitterType;

public enum PatternAdapterType {
    ToCycles,
    ToIntervals,
    ToObsoleteSamsungString,
    ToCyclesHtcPattern;

    public static @Nullable PatternAdapterType getConverterType(@Nullable TransmitterType transmitterType) {
        if (transmitterType == null) {
            return null;
        }

        switch (transmitterType) {
            case OBSOLETE_SAMSUNG:
                return PatternAdapterType.ToObsoleteSamsungString;
            case HTC:
                return PatternAdapterType.ToCyclesHtcPattern;
            default:
                // native & LG
                return PatternAdapterType.ToIntervals;
        }
    }
}
