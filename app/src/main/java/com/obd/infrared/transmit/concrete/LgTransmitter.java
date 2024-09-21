package com.obd.infrared.transmit.concrete;

import android.content.Context;
import android.util.Log;

import com.lge.hardware.IRBlaster.Device;
import com.lge.hardware.IRBlaster.IRAction;
import com.lge.hardware.IRBlaster.IRBlaster;
import com.lge.hardware.IRBlaster.IRBlasterCallback;
import com.lge.hardware.IRBlaster.IRFunction;
import com.lge.hardware.IRBlaster.ResultCode;
import com.obd.infrared.transmit.TransmitInfo;
import com.obd.infrared.transmit.Transmitter;

import java.util.ArrayList;
import java.util.List;

public abstract class LgTransmitter extends Transmitter implements IRBlasterCallback {

    private static final String TAG = "LgTransmitter";
    protected final IRBlaster irBlaster;
    private boolean isReady = false;

    public LgTransmitter(Context context) {
        super(context);
        Log.d(TAG, "Try to create LG IRBlaster");
        irBlaster = IRBlaster.getIRBlaster(context, this);
        Log.d(TAG, "IRBlaster created");
    }

    @Override
    public void start() {
        Log.d(TAG, "Start not supported in LG IRBlaster");
    }

    @Override
    public void transmit(TransmitInfo transmitInfo) {
        try {
            if (isReady) {
                beforeSendIr();
                Log.d(TAG, "Try to transmit LG IRBlaster");

                int resultCode = irBlaster.sendIRPattern(transmitInfo.frequency, transmitInfo.pattern);
                Log.d(TAG, "Result: " + ResultCode.getString(resultCode));
            } else {
                Log.w(TAG, "LG IRBlaster not ready");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error transmitting LG IRBlaster", e);
        }
    }

    protected abstract void beforeSendIr();

    @Override
    public void stop() {
        try {
            Log.d(TAG, "Try to close LG IRBlaster");
            irBlaster.close();
        } catch (Exception e) {
            Log.e(TAG, "Error closing LG IRBlaster", e);
        }
    }

    @Override
    public boolean isReady() {
        return isReady;
    }

    @Override
    public void IRBlasterReady() {
        isReady = true;
        Log.d(TAG, "LG IRBlaster ready");
    }

    @Override
    public void learnIRCompleted(int i) {
        Log.d(TAG, "LG IRBlaster.learnIRCompleted : " + i);
    }

    @Override
    public void newDeviceId(int i) {
        Log.d(TAG, "LG IRBlaster.newDeviceId : " + i);
    }

    @Override
    public void failure(int i) {
        Log.w(TAG, "LG IRBlaster.failure : " + i);
    }
}
