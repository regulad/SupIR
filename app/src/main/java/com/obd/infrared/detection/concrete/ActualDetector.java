package com.obd.infrared.detection.concrete;

import android.annotation.TargetApi;
import android.hardware.ConsumerIrManager;
import android.os.Build;
import android.util.Log;

import com.obd.infrared.detection.Detector;
import com.obd.infrared.detection.InfraRedDetector;
import com.obd.infrared.transmit.TransmitInfo;
import com.obd.infrared.transmit.TransmitterType;

import static android.content.Context.CONSUMER_IR_SERVICE;

public class ActualDetector implements Detector {

    private static final String TAG = "ActualDetector";

    @Override
    public boolean hasTransmitter(InfraRedDetector.DetectorUtils detectorUtils) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && hasActualIR(detectorUtils);
    }

    @SuppressWarnings("ResourceType")
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private boolean hasActualIR(InfraRedDetector.DetectorUtils detectorUtils) {
        try {
            Log.d(TAG, "Check CONSUMER_IR_SERVICE");
            ConsumerIrManager consumerIrManager = (ConsumerIrManager) detectorUtils.context.getSystemService(CONSUMER_IR_SERVICE);
            if (consumerIrManager != null && consumerIrManager.hasIrEmitter()) {

                logCarrierFrequencies(consumerIrManager);

                Log.d(TAG, "CONSUMER_IR_SERVICE: must be included TRANSMIT_IR permission to AndroidManifest.xml");
                TransmitInfo transmitInfo = new TransmitInfo(38000, new int[]{100, 100, 100, 100});
                consumerIrManager.transmit(transmitInfo.frequency, transmitInfo.pattern);

                Log.d(TAG, "CONSUMER_IR_SERVICE: hasIrEmitter is true");
                return true;
            } else {
                Log.d(TAG, "CONSUMER_IR_SERVICE: hasIrEmitter is false");
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "On actual transmitter error", e);
            return false;
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void logCarrierFrequencies(ConsumerIrManager consumerIrManager) {
        try {
            ConsumerIrManager.CarrierFrequencyRange[] carrierFrequencies = consumerIrManager.getCarrierFrequencies();
            if (carrierFrequencies != null && carrierFrequencies.length > 0) {
                for (ConsumerIrManager.CarrierFrequencyRange carrierFrequencyRange : carrierFrequencies) {
                    Log.d(TAG, "carrierFrequencyRange: MIN" + carrierFrequencyRange.getMinFrequency());
                    Log.d(TAG, "carrierFrequencyRange: MAX" + carrierFrequencyRange.getMaxFrequency());
                }
            } else {
                Log.d(TAG, "carrierFrequencies is empty or null");
            }
        } catch (Exception e) {
            Log.e(TAG, "getCarrierFrequencies", e);
        }
    }

    @Override
    public TransmitterType getTransmitterType() {
        return TransmitterType.ACTUAL_NATIVE;
    }
}
