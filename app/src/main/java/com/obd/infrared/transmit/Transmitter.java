package com.obd.infrared.transmit;

import android.content.Context;
import android.util.Log;
import androidx.annotation.Nullable;
import com.obd.infrared.detection.InfraRedDetector;
import com.obd.infrared.transmit.concrete.ActualTransmitter;
import com.obd.infrared.transmit.concrete.LeTransmitter;
import com.obd.infrared.transmit.concrete.ObsoleteSamsungTransmitter;

public abstract class Transmitter {
    private static final String TAG = "Transmitter";

    public static @Nullable Transmitter getTransmitterForDevice(Context context) {
        TransmitterType transmitterType = new InfraRedDetector(context).detect();

        Log.d(TAG, "Get transmitter by type: " + transmitterType);

        if (transmitterType == null) {
            return null;
        }

        switch (transmitterType) {
            case ACTUAL_NATIVE:
                return new ActualTransmitter(context);
            case OBSOLETE_SAMSUNG:
                return new ObsoleteSamsungTransmitter(context);
            case LE_COOLPAD:
                return new LeTransmitter(context);
            default:
                return null;
        }
    }

    protected final Context context;

    public Transmitter(Context context) {
        this.context = context;
    }

    public void start() {
    }

    public abstract void transmit(TransmitInfo transmitInfo);

    public boolean isReady() {
        return true;
    }

    public void stop() {
    }

    public TransmitterType getTransmitterType() {
        return TransmitterType.ACTUAL_NATIVE;
    }
}
