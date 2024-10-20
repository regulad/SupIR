package com.obd.infrared.detection;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import androidx.annotation.Nullable;
import com.obd.infrared.detection.concrete.ActualDetector;
import com.obd.infrared.detection.concrete.HtcDetector;
import com.obd.infrared.detection.concrete.LeDetector;
import com.obd.infrared.detection.concrete.ObsoleteSamsungDetector;
import com.obd.infrared.transmit.TransmitterType;

import java.util.ArrayList;
import java.util.List;

public class InfraRedDetector {
    private static final String TAG = "InfraRedDetector";

    public static class DetectorUtils {
        public final Context context;

        public DetectorUtils(Context context) {
            this.context = context;
        }

        public boolean hasAnyPackage(String... packageNames) {
            PackageManager manager = context.getPackageManager();
            List<ApplicationInfo> packages = manager.getInstalledApplications(0);
            for (ApplicationInfo info : packages) {
                for (String packageName : packageNames) {
                    if (info.packageName.contains(packageName)) {
                        return true;
                    }
                }
            }
            return false;
        }

    }

    protected final DetectorUtils detectorUtils;
    protected final List<Detector> detectors = new ArrayList<>();

    public InfraRedDetector(Context context) {
        this.detectorUtils = new DetectorUtils(context);

        this.detectors.add(new ActualDetector());
        this.detectors.add(new HtcDetector());
        this.detectors.add(new LeDetector());
        this.detectors.add(new ObsoleteSamsungDetector());
    }

    private @Nullable TransmitterType detect(DetectorUtils detectorUtils) {
        for (Detector detector : detectors) {
            // actualtransmitter is tested first
            if (detector.hasTransmitter(detectorUtils)) {
                return detector.getTransmitterType();
            }
        }
        return null;
    }

    public @Nullable TransmitterType detect() {
        TransmitterType transmitterType = detect(detectorUtils);
        Log.d(TAG, "Detected transmitter: " + transmitterType);
        return transmitterType;
    }
}
