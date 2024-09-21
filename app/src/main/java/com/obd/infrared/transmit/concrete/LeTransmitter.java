package com.obd.infrared.transmit.concrete;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.obd.infrared.transmit.TransmitInfo;
import com.obd.infrared.transmit.Transmitter;
import com.obd.infrared.transmit.TransmitterType;
import com.obd.infrared.utils.Constants;
import com.obd.infrared.utils.le.Device;
import com.obd.infrared.utils.le.IControl;
import com.obd.infrared.utils.le.IRAction;
import com.obd.infrared.utils.le.IRFunction;

import java.util.ArrayList;
import java.util.List;

public class LeTransmitter extends Transmitter {

    private static final String TAG = "LeTransmitter";

    private final ServiceConnection mControlServiceConnection = new ConnectorListener();
    private IControl remoteControl;

    public LeTransmitter(Context context) {
        super(context);
    }

    @Override
    public boolean isReady() {
        return remoteControl != null;
    }

    @Override
    public void transmit(TransmitInfo transmitInfo) {
        if (isReady()) {
            try {
                remoteControl.transmit(transmitInfo.frequency, transmitInfo.pattern);
                Log.d(TAG, "Transmission completed successfully");
            } catch (RemoteException e) {
                Log.e(TAG, "Error during transmission", e);
            }
        } else {
            Log.w(TAG, "Transmitter not ready");
        }
    }

    @Override
    public void start() {
        final String UEICONTROLPACKAGE = Build.BRAND.contains("Coolpad") ? Constants.LE_COOLPAD_IR_SERVICE_PACKAGE : Constants.LE_DEFAULT_IR_SERVICE_PACKAGE_2;

        try {
            Intent controlIntent = new Intent(IControl.DESCRIPTOR);
            controlIntent.setClassName(UEICONTROLPACKAGE, "com.uei.control.Service");
            context.bindService(controlIntent, this.mControlServiceConnection, Context.BIND_AUTO_CREATE);
            Log.d(TAG, "Service binding initiated");
        } catch (Exception e) {
            Log.e(TAG, "Error starting service", e);
        }
    }

    @Override
    public void stop() {
        try {
            context.unbindService(this.mControlServiceConnection);
            remoteControl = null;
            Log.d(TAG, "Service unbound successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error stopping service", e);
        }
    }

    private class ConnectorListener implements ServiceConnection {
        ConnectorListener() {
        }

        public void onServiceDisconnected(ComponentName name) {
            remoteControl = null;
            Log.d(TAG, "Service disconnected");
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            remoteControl = new IControl(service);
            Log.d(TAG, "Service connected");
        }
    }

    @Override
    public TransmitterType getTransmitterType() {
        return TransmitterType.LE_COOLPAD;
    }
}
