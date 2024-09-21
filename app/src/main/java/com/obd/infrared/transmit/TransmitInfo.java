package com.obd.infrared.transmit;

import org.jetbrains.annotations.NotNull;

public class TransmitInfo {

    public final int frequency;
    public final int[] pattern;
    public final Object[] obsoleteSamsungPattern;

    public TransmitInfo(int frequency, int[] pattern) {
        this.frequency = frequency;
        this.pattern = pattern;
        this.obsoleteSamsungPattern = null;
    }

    public TransmitInfo(int frequency, Object[] pattern) {
        this.frequency = frequency;
        this.pattern = null;
        this.obsoleteSamsungPattern = pattern;
    }

    @Override
    public @NotNull String toString() {
        StringBuilder stringBuilder = new StringBuilder("TransmitInfo [").append(frequency).append("]: ");
        if (pattern != null) {
            stringBuilder.append(" Count:").append(this.pattern.length).append(": ");
            for (int v : pattern) {
                stringBuilder.append(", ").append(v);
            }
        } else {
            stringBuilder.append(obsoleteSamsungPattern[0].toString());
        }
        return stringBuilder.toString();
    }
}
