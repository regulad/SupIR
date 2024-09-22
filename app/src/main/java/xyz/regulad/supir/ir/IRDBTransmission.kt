package xyz.regulad.supir.ir

import android.content.Context
import android.content.Context.CONSUMER_IR_SERVICE
import android.hardware.ConsumerIrManager
import android.hardware.ConsumerIrManager.CarrierFrequencyRange
import android.util.Log
import com.obd.infrared.patterns.PatternAdapter
import com.obd.infrared.patterns.PatternConverterUtils
import com.obd.infrared.patterns.PatternType
import com.obd.infrared.transmit.TransmitInfo
import com.obd.infrared.transmit.Transmitter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import xyz.regulad.supir.ir.IrEncoder.frequency
import xyz.regulad.supir.ir.IrEncoder.irpProtocolDefinition
import xyz.regulad.supir.ir.IrEncoder.timingString
import xyz.regulad.supir.ir.TransmitterManager.transmitSuspending
import java.util.*

fun CarrierFrequencyRange.contains(frequency: Int): Boolean {
    return frequency in minFrequency..maxFrequency
}

object TransmitterManager {
    private val transmitterMutexMap = Collections.synchronizedMap(WeakHashMap<Transmitter, Mutex>())

    suspend fun Transmitter.transmitSuspending(transmitInfo: TransmitInfo) {
        val mutex = transmitterMutexMap.getOrPut(this) { Mutex() }
        mutex.withLock {
            transmit(transmitInfo)
        }
    }

    private val protocolCompatibilityCache = mutableMapOf<String, Boolean>()

    fun IRDBFunction.isTransmittable(context: Context): Boolean =
        protocolCompatibilityCache.getOrPut(protocol) {
            return irpProtocolDefinition(context)?.frequency()?.let { freq ->
                // if the device has ConsumerIRManager, check to see if the frequency is supported
                // if the device does not have ConsumerIRManager, we can assume that the frequency is supported

                val irService = context.getSystemService(CONSUMER_IR_SERVICE) as ConsumerIrManager?
                irService == null || irService.carrierFrequencies.any { it.contains(freq.toInt()) }
            } ?: false
        }
}

/*
 * Transmit the IRDBFunction blockingly
 */
suspend fun IRDBFunction.transmit(context: Context, transmitter: Transmitter) {
    // we need to create the battery

    val (frequency, timingString) = timingString(context) ?: throw UnsupportedOperationException("Failed to get timing string for $protocol")

    Log.d("IRDBFunction", "$protocol $device $subdevice $function -> $timingString")

    val patternConverter = PatternConverterUtils.fromString(PatternType.Intervals, frequency.toInt(), timingString)

    val patternAdapter = PatternAdapter(transmitter.transmitterType)

    val transmitInfo = patternAdapter.createTransmitInfo(patternConverter)

    // this is a blocking call: we do not need to repeat; the timing string includes a repeat (except special cases)

    val repeats = when (protocol.uppercase()) {
        "NEC" -> 2
        "NECX" -> 2
        else -> 1
    }

    repeat(repeats) {
        transmitter.transmitSuspending(transmitInfo)
    }
}
