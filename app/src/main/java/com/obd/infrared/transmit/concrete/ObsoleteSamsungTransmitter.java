package com.obd.infrared.transmit.concrete;

import android.content.Context;
import android.util.Log;

import com.obd.infrared.transmit.TransmitInfo;
import com.obd.infrared.transmit.Transmitter;
import com.obd.infrared.transmit.TransmitterType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ObsoleteSamsungTransmitter extends Transmitter {

    private static final String TAG = "ObsoleteTransmitter";

    private final Object irdaService;
    private Method write_irsend;

    @SuppressWarnings("ResourceType")
    public ObsoleteSamsungTransmitter(Context context) {
        super(context);
        Log.d(TAG, "Try to create ObsoleteTransmitter");
        irdaService = context.getSystemService("irda");
        try {
            write_irsend = irdaService.getClass().getMethod("write_irsend", String.class);
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "NoSuchMethodException", e);
        }
        Log.d(TAG, "ObsoleteTransmitter created");
    }

    @Override
    public void transmit(TransmitInfo transmitInfo) {
        try {
            write_irsend.invoke(irdaService, transmitInfo.obsoleteSamsungPattern);
            Log.d(TAG, "IR signal transmitted successfully");
        } catch (IllegalAccessException e) {
            Log.e(TAG, "IllegalAccessException", e);
        } catch (InvocationTargetException e) {
            Log.e(TAG, "InvocationTargetException", e);
        }
    }

    @Override
    public TransmitterType getTransmitterType() {
        return TransmitterType.OBSOLETE_SAMSUNG;
    }
}
