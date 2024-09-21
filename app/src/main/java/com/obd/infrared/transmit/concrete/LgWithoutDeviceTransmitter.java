package com.obd.infrared.transmit.concrete;

import android.content.Context;
import com.obd.infrared.transmit.TransmitterType;

public class LgWithoutDeviceTransmitter extends LgTransmitter {
    public LgWithoutDeviceTransmitter(Context context) {
        super(context);
    }

    @Override
    protected void beforeSendIr() {
        // Do nothing before send ir command
    }

    @Override
    public TransmitterType getTransmitterType() {
        return TransmitterType.LG_WITHOUT_DEVICE;
    }
}
