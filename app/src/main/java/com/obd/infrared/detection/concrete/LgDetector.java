package com.obd.infrared.detection.concrete;

import android.util.Log;

import com.lge.hardware.IRBlaster.IRBlaster;
import com.obd.infrared.detection.Detector;
import com.obd.infrared.detection.InfraRedDetector;
import com.obd.infrared.transmit.TransmitterType;

public class LgDetector implements Detector {

    private static final String TAG = "LgDetector";
    private final TransmitterType transmitterType = TransmitterType.LG;

    @Override
    public boolean hasTransmitter(InfraRedDetector.DetectorUtils detectorUtils) {
        try {
            // see if the class exists
            if (hasLGService()) {
                boolean isSdkSupported = IRBlaster.isSdkSupported(detectorUtils.context);
                Log.d(TAG, "Check LG IRBlaster " + isSdkSupported);
                return isSdkSupported;
            } else {
                Log.d(TAG, "Not an LG device");
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "On LG IR detection error", e);
            return false;
        }
    }

    private boolean hasLGService() {
        try {
            Class.forName("com.lge.hardware.IRBlaster.IRBlaster");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public TransmitterType getTransmitterType() {
        return transmitterType;
    }
}
