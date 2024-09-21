package com.obd.infrared.transmit.concrete;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.htc.circontrol.CIRControl;
import com.htc.htcircontrol.HtcIrData;
import com.obd.infrared.transmit.TransmitInfo;
import com.obd.infrared.transmit.Transmitter;
import com.obd.infrared.transmit.TransmitterType;

public class HtcTransmitter extends Transmitter {
    private static final String TAG = "HtcTransmitter";

    private class SendRunnable implements Runnable {
        private int frequency;
        private int[] frame;

        public SendRunnable(int frequency, int[] frame) {
            this.frequency = frequency;
            this.frame = frame;
        }

        public void run() {
            try {
                htcControl.transmitIRCmd(new HtcIrData(1, frequency, frame), false);
            } catch (IllegalArgumentException iae) {
                Log.e(TAG, "Run: IllegalArgumentException", iae);
            } catch (Exception e) {
                Log.e(TAG, "Run: Exception", e);
            }
        }
    }

    private static class HtcHandler extends Handler {
        public HtcHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "HtcHandler.handleMessage:");
            Log.d(TAG, "msg.what: " + msg.what + " arg1: " + msg.arg1 + " arg2: " + msg.arg2);
            Log.d(TAG, "msg.toString: " + msg.toString());

            switch (msg.what) {
                case CIRControl.MSG_RET_LEARN_IR:
                    Log.d(TAG, "MSG_RET_LEARN_IR");
                    break;
                case CIRControl.MSG_RET_TRANSMIT_IR:
                    Log.d(TAG, "MSG_RET_TRANSMIT_IR");
                    switch (msg.arg1) {
                        case CIRControl.ERR_IO_ERROR:
                            Log.e(TAG, "CIR hardware component is busy in doing early CIR command");
                            Log.e(TAG, "Send IR Error=ERR_IO_ERROR");
                            break;
                        case CIRControl.ERR_INVALID_VALUE:
                            Log.e(TAG, "Send IR Error=ERR_INVALID_VALUE");
                            break;
                        case CIRControl.ERR_CMD_DROPPED:
                            Log.w(TAG, "SDK might be too busy to send IR key, developer can try later, or send IR key with non-droppable setting");
                            Log.w(TAG, "Send IR Error=ERR_CMD_DROPPED");
                            break;
                        default:
                            Log.d(TAG, "default");
                            break;
                    }
                    break;
                case CIRControl.MSG_RET_CANCEL:
                    Log.d(TAG, "MSG_RET_CANCEL");
                    switch (msg.arg1) {
                        case CIRControl.ERR_IO_ERROR:
                            Log.e(TAG, "CIR hardware component is busy in doing early CIR command");
                            Log.e(TAG, "Send IR Error=ERR_IO_ERROR");
                            break;
                        case CIRControl.ERR_CANCEL_FAIL:
                            Log.e(TAG, "CIR hardware component is busy in doing early CIR command");
                            Log.e(TAG, "Cancel Error: ERR_CANCEL_FAIL");
                            break;
                        default:
                            Log.d(TAG, "default");
                            break;
                    }
                    break;
                default:
                    Log.d(TAG, "global default");
            }
        }
    }

    private final CIRControl htcControl;
    private final Handler htcHandler;

    public HtcTransmitter(Context context) {
        super(context);

        Log.d(TAG, "Try to create HtcTransmitter");
        htcHandler = new HtcHandler(Looper.getMainLooper());
        htcControl = new CIRControl(context, htcHandler);
        Log.d(TAG, "HtcTransmitter created");
    }

    @Override
    public void start() {
        try {
            Log.d(TAG, "Try to start HTC CIRControl");
            htcControl.start();
        } catch (Exception e) {
            Log.e(TAG, "On try to start HTC CIRControl", e);
        }
    }

    @Override
    public void transmit(TransmitInfo transmitInfo) {
        try {
            if (htcControl.isStarted()) {
                Log.d(TAG, "Try to transmit HTC");
                htcHandler.post(new SendRunnable(transmitInfo.frequency, transmitInfo.pattern));
            } else {
                Log.w(TAG, "htcControl not started");
            }
        } catch (Exception e) {
            Log.e(TAG, "On try to transmit", e);
        }
    }

    @Override
    public void stop() {
        try {
            Log.d(TAG, "Try to stop HTC CIRControl");
            htcControl.stop();
        } catch (Exception e) {
            Log.e(TAG, "On try to stop HTC CIRControl", e);
        }
    }

    @Override
    public TransmitterType getTransmitterType() {
        return TransmitterType.HTC;
    }
}
