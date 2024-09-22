package xyz.regulad.supir.ir

import android.os.Parcelable
import android.util.Log
import com.obd.infrared.patterns.PatternAdapter
import com.obd.infrared.patterns.PatternConverterUtils
import com.obd.infrared.patterns.PatternType
import com.obd.infrared.transmit.Transmitter
import kotlinx.parcelize.Parcelize
import java.lang.UnsupportedOperationException

const val defaultFrequency = 38400

val protocolFrequencyMap = mapOf(
    "DAC4" to 38000,
    "Dell" to 36000,
    "Denon-K" to 37000,
    "Dgtec" to 38000,
    "DishPlayer_Network" to 57600,
    "Dreambox" to 38000,
    "Furby" to 40000,
    "GI4dtv" to 37700,
    "GI_cable" to 38400,
    "Jerrold" to 0,
    "Kaseikyo" to 37000,
    "Kramer" to 38462,
    "Mitsubishi" to 32600,
    "NECx1" to 38000,
    "NECx2" to 38000,
    "Nokia32" to 36000,
    "Nokia32single" to 36000,
    "Polycom" to 38740,
    "Proton" to 38000,
    "Samsung20" to 38400,
    "Samsung36" to 38000,
    "TViX" to 38000,
    "Teac-K" to 37900,
    "Thomson" to 33000,
    "Tivo-Nec1" to 38000,
    "XMP" to 38000,
    "aiwa" to 38000,
    "async" to 43600,
    "blaupunkt" to 30500,
    "denon" to 37917,
    "emerson" to 36700,
    "f12" to 38000,
    "fujitsu" to 38000,
    "iPod" to 38000,
    "imonpc" to 39700,
    "jvc" to 37900,
    "jvc_two_frames" to 37900,
    "lumagen" to 38000,
    "mce" to 36000,
    "nec1" to 38000,
    "nec2" to 38000,
    "panasonic" to 37000,
    "panasonic2" to 37000,
    "pioneer" to 40000,
    "pioneer2" to 40000,
    "rc5" to 36000,
    "rc5odd" to 36000,
    "rc5x" to 36000,
    "rc6-M-L" to 36000,
    "rc6" to 36000,
    "rca" to 58000,
    "recs80_45" to 38000,
    "recs80_68" to 36400,
    "russound" to 38400,
    "sagem" to 56000,
    "sharp" to 37917,
    "streamzap" to 59000,
    "x10ir" to 40000
)

private fun MutableList<Int>.addNECBits(value: Int, bitCount: Int) {
    for (i in bitCount - 1 downTo 0) {
        if ((value and (1 shl i)) != 0) {
            // One bit
            this.add(21)
            this.add(64)
        } else {
            // Zero bit
            this.add(21)
            this.add(21)
        }
    }
}

@Parcelize
data class IRDBFunction(
    val functionName: String,
    val protocol: String,
    val device: Int,
    val subdevice: Int,
    val function: Int,
) : Parcelable {
    private val correctedProtocol: String get() {
        if (protocol.startsWith("RC6")) {
            return "RC6"
        }

        return when (protocol.uppercase()) {
            "NEC" -> "NEC2"
            "NECX" -> "NEC1"
            else -> protocol
        }
    }

    private val frequency: Int get() {
        return protocolFrequencyMap.mapKeys { it.key.uppercase() }[correctedProtocol] ?: defaultFrequency
    }

    val transmittable: Boolean get() {
        return timingString != null
    }

    /**
     * Returns the timing strings in pulses and spaces
     */
    private val timingString: List<Int>? get() {
        return when (correctedProtocol.uppercase()) {
            "NEC2", "NEC1" -> {
                val timingList = mutableListOf<Int>()

                // Leader
                timingList.add(342)
                timingList.add(171)

                // Address
                val address = if (correctedProtocol.uppercase() == "NEC1") {
                    device
                } else {
                    (device shl 8) or subdevice
                }
                timingList.addNECBits(address, 16)

                // Inverted Address
                val invertedAddress = if (correctedProtocol.uppercase() == "NEC1") {
                    subdevice
                } else {
                    address.inv() and 0xFFFF
                }
                timingList.addNECBits(invertedAddress, 16)

                // Command
                timingList.addNECBits(function, 8)

                // Inverted Command
                timingList.addNECBits(function.inv() and 0xFF, 8)

                // Trailing pulse
                timingList.add(21)

                timingList
            }
            else -> null
        }
    }

    fun transmit(transmitter: Transmitter) {
        // we need to create the battery

        if (!transmittable) {
            throw UnsupportedOperationException("Protocol $protocol is not transmittable")
        }

        val timingString = timingString ?: throw UnsupportedOperationException("Failed to get timing string for $protocol")

        Log.d("IRDBFunction", "Transmitting $protocol with timing string $timingString")

        val patternConverter = PatternConverterUtils.fromString(PatternType.Intervals, frequency, timingString.joinToString(" "))

        val patternAdapter = PatternAdapter(transmitter.transmitterType)

        val transmitInfo = patternAdapter.createTransmitInfo(patternConverter)

        // this is a blocking call: we do not need to repeat; the timing string includes a repeat
        transmitter.transmit(transmitInfo)
    }
}
