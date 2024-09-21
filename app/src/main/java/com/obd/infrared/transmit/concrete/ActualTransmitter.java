package com.obd.infrared.transmit.concrete;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.ConsumerIrManager;
import android.os.Build;
import android.util.Log;

import com.obd.infrared.transmit.TransmitInfo;
import com.obd.infrared.transmit.Transmitter;
import com.obd.infrared.transmit.TransmitterType;

import static android.content.Context.CONSUMER_IR_SERVICE;

public class ActualTransmitter extends Transmitter {

    private static final String TAG = "ActualTransmitter";
    private final ConsumerIrManager consumerIrManager;

    public ActualTransmitter(Context context) {
        super(context);
        Log.d(TAG, "Try to create ActualTransmitter");
        this.consumerIrManager = getConsumerIrManager();
        Log.d(TAG, "ActualTransmitter created");
    }

    @SuppressWarnings("ResourceType")
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private ConsumerIrManager getConsumerIrManager() {
        return (ConsumerIrManager) context.getSystemService(CONSUMER_IR_SERVICE);
    }

    @Override
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void transmit(TransmitInfo transmitInfo) {
        Log.d(TAG, "Try to transmit");
        try {
            consumerIrManager.transmit(transmitInfo.frequency, transmitInfo.pattern);
            Log.d(TAG, "Transmission completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error during transmission", e);
        }
    }

    @Override
    public TransmitterType getTransmitterType() {
        return TransmitterType.ACTUAL_NATIVE;
    }
}
