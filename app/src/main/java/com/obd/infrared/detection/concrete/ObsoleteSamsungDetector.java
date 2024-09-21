package com.obd.infrared.detection.concrete;

import android.util.Log;

import com.obd.infrared.detection.Detector;
import com.obd.infrared.detection.InfraRedDetector;
import com.obd.infrared.transmit.TransmitterType;

import java.lang.reflect.Method;

public class ObsoleteSamsungDetector implements Detector {

    private static final String TAG = "ObsoleteSamsungDetector";

    @SuppressWarnings({"ResourceType", "SpellCheckingInspection", "RedundantArrayCreation"})
    @Override
    public boolean hasTransmitter(InfraRedDetector.DetectorUtils detectorUtils) {
        try {
            Log.d(TAG, "Check obsolete Samsung IR interface");
            Object irdaService = detectorUtils.context.getSystemService("irda");
            if (irdaService == null) {
                Log.d(TAG, "Not found obsolete Samsung IR service");
                return false;
            }
            Log.d(TAG, "Got irdaService");
            Method write_irsend = irdaService.getClass().getMethod("write_irsend", new Class[]{String.class});
            Log.d(TAG, "Got write_irsend");

            Log.d(TAG, "Try to send IR command");
            write_irsend.invoke(irdaService, "38000,100,100,100,100");
            Log.d(TAG, "Called write_irsend.invoke");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "On obsolete transmitter error", e);
            return false;
        }
    }

    @Override
    public TransmitterType getTransmitterType() {
        return TransmitterType.OBSOLETE_SAMSUNG;
    }
}
