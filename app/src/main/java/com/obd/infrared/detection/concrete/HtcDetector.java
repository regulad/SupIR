package com.obd.infrared.detection.concrete;

import android.util.Log;

import com.obd.infrared.detection.Detector;
import com.obd.infrared.detection.InfraRedDetector;
import com.obd.infrared.transmit.TransmitterType;

public class HtcDetector implements Detector {

    private static final String TAG = "HtcDetector";

    /**
     * Code from samples in HTC IR SDK
     */
    @Override
    public boolean hasTransmitter(InfraRedDetector.DetectorUtils detectorUtils) {
        try {
            boolean hasPackage = detectorUtils.hasAnyPackage("com.htc.cirmodule");
            Log.d(TAG, "Check HTC IR interface: " + hasPackage);
            return hasPackage;
        } catch (Exception e) {
            Log.e(TAG, "On HTC IR error", e);
            return false;
        }
    }

    @Override
    public TransmitterType getTransmitterType() {
        return TransmitterType.HTC;
    }
}
