package xyz.regulad.supir.ir

import android.content.Context
import android.util.Log
import com.obd.infrared.patterns.PatternAdapter
import com.obd.infrared.patterns.PatternConverterUtils
import com.obd.infrared.patterns.PatternType
import com.obd.infrared.transmit.Transmitter
import xyz.regulad.supir.ir.IrEncoder.timingString
import java.lang.UnsupportedOperationException

fun IRDBFunction.isTransmittable(context: Context): Boolean {
    return timingString(context) != null
}

fun IRDBFunction.transmit(context: Context, transmitter: Transmitter) {
    // we need to create the battery

    val (frequency, timingString) = timingString(context) ?: throw UnsupportedOperationException("Failed to get timing string for $protocol")

    Log.d("IRDBFunction", "$protocol $device $subdevice $function -> $timingString")

    val patternConverter = PatternConverterUtils.fromString(PatternType.Intervals, frequency.toInt(), timingString)

    val patternAdapter = PatternAdapter(transmitter.transmitterType)

    val transmitInfo = patternAdapter.createTransmitInfo(patternConverter)

    // this is a blocking call: we do not need to repeat; the timing string includes a repeat
    transmitter.transmit(transmitInfo)
}
