package xyz.regulad.supir

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.obd.infrared.transmit.Transmitter

class SupIRViewModel(application: Application) : AndroidViewModel(application) {
    val transmitter: Transmitter? = Transmitter.getTransmitterForDevice(application)
}
