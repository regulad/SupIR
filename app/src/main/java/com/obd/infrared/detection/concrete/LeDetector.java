package com.obd.infrared.detection.concrete;

import android.util.Log;
import com.obd.infrared.detection.Detector;
import com.obd.infrared.detection.InfraRedDetector;
import com.obd.infrared.transmit.TransmitterType;
import com.obd.infrared.utils.Constants;

/**
 * Created by Andrew on 20.10.2017
 */

public class LeDetector implements Detector {

    private static final String TAG = "LeDetector";

    @Override
    public boolean hasTransmitter(InfraRedDetector.DetectorUtils detectorUtils) {
        try {
            boolean hasPackage = detectorUtils.hasAnyPackage(Constants.LE_COOLPAD_IR_SERVICE_PACKAGE, Constants.LE_DEFAULT_IR_SERVICE_PACKAGE_2);
            Log.d(TAG, "Check Le IR interface: " + hasPackage);
            return hasPackage;
        } catch (Exception e) {
            Log.e(TAG, "On Le IR error", e);
            return false;
        }
    }

    @Override
    public TransmitterType getTransmitterType() {
        return TransmitterType.LE_COOLPAD;
    }
}
