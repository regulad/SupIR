package xyz.regulad.supir.ir

import android.util.Log
import com.obd.infrared.patterns.PatternAdapter
import com.obd.infrared.patterns.PatternConverterUtils
import com.obd.infrared.patterns.PatternType
import com.obd.infrared.transmit.Transmitter
import com.obd.infrared.transmit.TransmitterType
import xyz.regulad.makehex.IRDBFunction
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

private val uppercaseProtocolFrequencyMap = protocolFrequencyMap.mapKeys { it.key.uppercase() }

val IRDBFunction.frequency: Int get() {
    // case insensitive
    return uppercaseProtocolFrequencyMap[protocol.uppercase()] ?: defaultFrequency
}

/**
 * Returns true if the IRDBFunction supports transmission
 */
val IRDBFunction.transmittable: Boolean get() {
    return protocol.uppercase() in uppercaseProtocolFrequencyMap || protocol.uppercase() == "NEC" || protocol.uppercase() == "NECX" || protocol.startsWith("RC6")
}

fun IRDBFunction.transmit(transmitter: Transmitter) {
    // we need to create the battery

    if (!transmittable) {
        throw UnsupportedOperationException("Protocol $protocol is not transmittable")
    }

    val timingString = timingString() ?: throw UnsupportedOperationException("Failed to get timing string for $protocol")

    Log.d("IRDBFunction", "Transmitting $protocol with timing string $timingString")

    val patternConverter = PatternConverterUtils.fromString(PatternType.Intervals, frequency, timingString)

    val patternAdapter = PatternAdapter(transmitter.transmitterType)

    val transmitInfo = patternAdapter.createTransmitInfo(patternConverter)

    // this is a blocking call
    transmitter.transmit(transmitInfo)
}
