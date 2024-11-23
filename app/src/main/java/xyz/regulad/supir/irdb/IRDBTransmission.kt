package xyz.regulad.supir.irdb

import android.content.Context
import android.content.Context.CONSUMER_IR_SERVICE
import android.hardware.ConsumerIrManager
import android.hardware.ConsumerIrManager.CarrierFrequencyRange
import android.util.Log
import com.obd.infrared.patterns.PatternAdapter
import com.obd.infrared.patterns.PatternConverter
import com.obd.infrared.patterns.PatternType
import com.obd.infrared.transmit.TransmitInfo
import com.obd.infrared.transmit.Transmitter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import xyz.regulad.supir.irdb.IrEncoder.getFrequency
import xyz.regulad.supir.irdb.IrEncoder.initialTimingString
import xyz.regulad.supir.irdb.IrEncoder.repeatTimingString
import xyz.regulad.supir.irdb.TransmitterManager.transmitSuspending
import java.util.*

fun CarrierFrequencyRange.contains(frequency: Int): Boolean {
    return frequency in minFrequency..maxFrequency
}

object TransmitterManager {
    private val transmitterMutexMap = Collections.synchronizedMap(WeakHashMap<Transmitter, Mutex>())

    suspend fun Transmitter.transmitSuspending(transmitInfo: TransmitInfo) {
        val mutex = transmitterMutexMap.getOrPut(this) { Mutex() }
        mutex.withLock {
            withContext(Dispatchers.IO) {
                transmit(transmitInfo) // hard block, run on dispatcher
            }
        }
    }

    private val protocolCompatibilityCache = mutableMapOf<String, Boolean>()

    fun IRDBFunction.isTransmittable(context: Context): Boolean =
        // dedupe mixed case protocols, they are all the same
        protocolCompatibilityCache.getOrPut(protocol.uppercase()) {
            // as far as frequency is concerned, the IRP is equivalent for any members of the same protocol
            Log.d("TransmitterManager", "Checking compatibility for $protocol")
            getFrequency(context)?.let { freq ->
                // if the device has ConsumerIRManager, check to see if the frequency is supported
                // if the device does not have ConsumerIRManager, we can assume that the frequency is supported

                val irService = context.getSystemService(CONSUMER_IR_SERVICE) as ConsumerIrManager?
                irService == null || irService.carrierFrequencies.any { it.contains(freq.toInt()) }
            } ?: false
        }
}

suspend fun Transmitter.transmitMicrosecondIntArray(frequency: Int, pattern: IntArray) {
    val patternConverter =
        PatternConverter(PatternType.Intervals, frequency, *pattern)

    val patternAdapter = PatternAdapter(transmitterType)

    val transmitInfo = patternAdapter.createTransmitInfo(patternConverter)

    // this is a blocking call: we do not need to repeat; the timing string includes a repeat (except special cases)

    transmitSuspending(transmitInfo)
}

/*
 * Transmit the IRDBFunction blockingly
 */
suspend fun IRDBFunction.transmitInitialPattern(context: Context, transmitter: Transmitter) {
    // we need to create the battery

    val (frequency, timingString) = initialTimingString(context)
        ?: throw UnsupportedOperationException("Failed to get timing string for $protocol")

    Log.d("IRDBFunction", "$protocol (init) $device $subdevice $function -> ${timingString.joinToString(" ")}")

    transmitter.transmitMicrosecondIntArray(frequency.toInt(), timingString.map { it.toInt() }.toIntArray())
}

suspend fun IRDBFunction.transmitRepeatPattern(context: Context, transmitter: Transmitter) {
    // we need to create the battery

    val (frequency, timingString) = repeatTimingString(context)
        ?: throw UnsupportedOperationException("Failed to get timing string for $protocol")

    Log.d("IRDBFunction", "$protocol (rep.) $device $subdevice $function -> ${timingString.joinToString(" ")}")

    transmitter.transmitMicrosecondIntArray(frequency.toInt(), timingString.map { it.toInt() }.toIntArray())
}
