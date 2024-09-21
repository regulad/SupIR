package com.obd.infrared.detection;


import com.obd.infrared.transmit.TransmitterType;

public interface Detector {
    boolean hasTransmitter(InfraRedDetector.DetectorUtils detectorUtils);
    TransmitterType getTransmitterType();
}
