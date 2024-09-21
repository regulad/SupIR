package com.obd.infrared.transmit.concrete;

import android.content.Context;
import android.util.Log;

import com.lge.hardware.IRBlaster.Device;
import com.lge.hardware.IRBlaster.IRAction;
import com.lge.hardware.IRBlaster.IRFunction;
import com.lge.hardware.IRBlaster.ResultCode;
import com.obd.infrared.transmit.TransmitterType;

public class LgWithDeviceTransmitter extends LgTransmitter {

    private static final String TAG = "LgWithDeviceTransmitter";
    private Device deviceSelected = null;

    public LgWithDeviceTransmitter(Context context) {
        super(context);
    }

    @Override
    public void IRBlasterReady() {
        super.IRBlasterReady();
        Device[] mDevices = irBlaster.getDevices();
        for (Device device : mDevices) {
            if (device.KeyFunctions != null && device.KeyFunctions.size() > 0) {
                deviceSelected = device;
                break;
            }
        }
        // logDevices(mDevices);
        Log.d(TAG, "LG deviceSelected: " + (deviceSelected != null));
    }

    @SuppressWarnings("unused")
    private void logDevices(Device[] mDevices) {
        for(Device device: mDevices) {
            Log.d(TAG, "Device ID:" + device.Id + " Name:" + device.Name + " B: " + device.Brand +
                    " T:" + device.DeviceTypeName + " XZ: " + device.origName + " XZ: " +
                    device.transName + " X: " + device.KeyFunctions.size());
            if (device.Name.contains("LG")) {
                int s = 0;
                for (IRFunction k : device.KeyFunctions) {
                    Log.d(TAG, "Name: " + k.Name + " id: " + k.Id + " is: " + k.IsLearned);
                    s++;
                    if (s > 10) {
                        break;
                    }
                }
            }
        }
    }

    @Override
    protected void beforeSendIr() {
        if (deviceSelected != null) {
            int resultCode = irBlaster.sendIR(new IRAction(deviceSelected.Id, deviceSelected.KeyFunctions.get(0).Id, 0));
            Log.d(TAG, "Try to IRBlaster.send known IR pattern. Result: " + ResultCode.getString(resultCode));
        } else {
            Log.w(TAG, "No device selected for sending IR pattern");
        }
    }

    @Override
    public TransmitterType getTransmitterType() {
        return TransmitterType.LG;
    }
}
