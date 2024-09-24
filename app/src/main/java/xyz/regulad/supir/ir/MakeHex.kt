package xyz.regulad.supir.ir

import android.content.Context
import android.util.Log
import java.util.*
import kotlin.experimental.inv
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

/**
 * IRP class for handling Infrared Remote Protocol
 * http://www.hifi-remote.com/wiki/index.php/IRP_Notation
 */
class IRP(
    var frequency: Double = 38400.0,
    var timeBase: Double = 1.0,
    var messageTime: Double = 0.0,
    var prefix: String? = null,
    var suffix: String? = null,
    var rPrefix: String? = null,
    var rSuffix: String? = null,
    var msb: Boolean = false,
    var form: String? = null,
    var numberFormat: Int = 0,
    var bitGroup: Int = 2
) {
    companion object {
        enum class Precedence { UNARY, TIMES, PLUS, COLON }
        data class Value(var value: Double, var bits: Int)
        data class IRPConfig(
            val digits: Array<String?>,
            val def: Array<String?>,
            val value: IntArray,
            val device: IntArray,
            val functions: IntArray,
            val mask: IntArray
        )

        private fun reverse(number: Int): Int = number.let { n ->
            var result = n
            result = ((result and 0x55555555) shl 1) or ((result ushr 1) and 0x55555555)
            result = ((result and 0x33333333) shl 2) or ((result ushr 2) and 0x33333333)
            result = ((result and 0x0F0F0F0F) shl 4) or ((result ushr 4) and 0x0F0F0F0F)
            result = ((result and 0x00FF00FF) shl 8) or ((result ushr 8) and 0x00FF00FF)
            (result ushr 16) or (result shl 16)
        }

        private fun createMask(): IntArray {
            val mask = IntArray(33)
            mask[0] = 0
            (1..32).forEach { mask[it] = 2 * mask[it - 1] + 1 }
            return mask
        }
    }

    internal val config = IRPConfig(
        digits = arrayOfNulls(16),
        def = arrayOfNulls(26),
        value = IntArray(26),
        device = intArrayOf(-1, -1),
        functions = intArrayOf(-1, -1, -1, -1),
        mask = createMask()
    )

    fun readIrpString(str: String): Boolean {
        str.lines().filter { it.isNotEmpty() }.forEach { line ->
            val cleanLine = line.uppercase().replace(Regex("\\s+"), "").substringBefore("'")
            try {
                processLine(cleanLine)
            } catch (e: Exception) {
                Log.e("IRP", "Error processing line: $line", e)
            }
        }

        if (config.device[1] >= 0) config.def['S' - 'A'] = null
        if (config.functions[1] >= 0) config.def['N' - 'A'] = null

        return form != null &&
                config.digits[0] != null &&
                config.digits[1] != null &&
                config.functions[0] != -1 &&
                !(config.functions[2] >= 0 && config.functions[2] != config.functions[0] && config.functions[3] != config.functions[1])
    }

    private fun processLine(line: String) {
        when {
            line.startsWith("FREQUENCY=") -> frequency = parseVal(line.substringAfter("=")).value
            line.startsWith("TIMEBASE=") -> timeBase = parseVal(line.substringAfter("=")).value
            line.startsWith("MESSAGETIME=") -> {
                val value = parseVal(line.substringAfter("="))
                messageTime = if (value.bits == 0) value.value * timeBase else value.value
            }
            line[0] in '0'..'9' && line[1] == '=' -> setDigit(line[0] - '0', line.substring(2))
            line.startsWith("1") && line[1] in '0'..'5' && line[2] == '=' -> setDigit(line[1] - '0' + 10, line.substring(3))
            line.startsWith("ZERO=") -> setDigit(0, line.substringAfter("="))
            line.startsWith("ONE=") -> setDigit(1, line.substringAfter("="))
            line.startsWith("TWO=") -> setDigit(2, line.substringAfter("="))
            line.startsWith("THREE=") -> setDigit(3, line.substringAfter("="))
            line.startsWith("PREFIX=") -> prefix = line.substringAfter("=")
            line.startsWith("SUFFIX=") -> suffix = line.substringAfter("=")
            line.startsWith("R-PREFIX=") -> rPrefix = line.substringAfter("=")
            line.startsWith("R-SUFFIX=") -> rSuffix = line.substringAfter("=")
            line.startsWith("FIRSTBIT=MSB") -> msb = true
            line.startsWith("FORM=") -> form = line.substringAfter("=")
            line.startsWith("DEFINE") || line.startsWith("DEFAULT") -> handleDefine(line.substringAfter("DEFINE").substringAfter("DEFAULT"))
            line.startsWith("DEVICE=") -> getPair(config.device, line.substringAfter("="))
            line.startsWith("FUNCTION=") -> handleFunction(line.substringAfter("="))
        }
    }

    private fun setDigit(d: Int, value: String) {
        config.digits[d] = value
        while (d >= bitGroup) bitGroup = bitGroup shl 1
    }

    private fun handleDefine(input: String) {
        when {
            input[1] == '=' -> config.def[input[0] - 'A'] = input.substring(2)
            input[0] == '=' && input.substring(2).startsWith("AS") -> config.def[input[2] - 'A'] = input.substring(4)
            input.substring(1).startsWith("AS") -> config.def[input[0] - 'A'] = input.substring(3)
        }
    }

    private fun handleFunction(input: String) {
        getPair(config.functions, input)
        if (input.startsWith("..")) {
            getPair(config.functions.copyOfRange(2, 4), input.substring(2))
        }
    }

    private fun parseVal(input: String, prec: Precedence = Precedence.UNARY): Value {
        var result = Value(0.0, 0)
        var i = 0

        when {
            input[i] in 'A'..'Z' -> {
                val ndx = input[i] - 'A'
                i++
                config.def[ndx]?.let { result = parseVal(it) } ?: run { result.value = config.value[ndx].toDouble() }
            }
            input[i] in '0'..'9' -> {
                result.value = input.substring(i).takeWhile { it.isDigit() }.toDouble()
                i += result.value.toString().length
            }
            input[i] == '-' -> {
                i++
                val temp = parseVal(input.substring(i), Precedence.UNARY)
                result.value = -temp.value
                result.bits = if (temp.bits > 0) 0 else temp.bits
            }
            input[i] == '~' -> {
                i++
                val temp = parseVal(input.substring(i), Precedence.UNARY)
                result.value = temp.value.toInt().toByte().inv().toDouble()
                if (temp.bits > 0) {
                    result.value = (result.value.toInt() and config.mask[temp.bits]).toDouble()
                    result.bits = temp.bits
                }
            }
            input[i] == '(' -> {
                val closingIndex = input.indexOf(')', i)
                if (closingIndex == -1) throw IllegalArgumentException("Mismatched parentheses")
                result = parseVal(input.substring(i + 1, closingIndex))
                i = closingIndex + 1
            }
        }

        when {
            i < input.length && input[i] == 'M' -> {
                result.value *= 1000
                result.bits = -1
                i++
            }
            i < input.length && input[i] == 'U' -> {
                result.bits = -1
                i++
            }
        }

        while (i < input.length) {
            when {
                prec.ordinal < Precedence.TIMES.ordinal && input[i] == '*' -> {
                    i++
                    val temp = parseVal(input.substring(i), Precedence.TIMES)
                    result.value *= temp.value
                    if (result.bits > 0) result.bits = 0
                }
                prec.ordinal < Precedence.PLUS.ordinal && input[i] in setOf('+', '-', '^') -> {
                    val op = input[i]
                    i++
                    val temp = parseVal(input.substring(i), Precedence.PLUS)
                    when (op) {
                        '+' -> result.value += temp.value
                        '-' -> result.value -= temp.value
                        '^' -> {
                            result.value = (result.value.toInt() xor temp.value.toInt()).toDouble()
                            if (result.bits > 0 && (temp.bits <= 0 || temp.bits > result.bits)) {
                                result.bits = temp.bits
                            }
                        }
                    }
                    if (result.bits > 0) result.bits = 0
                }
                prec.ordinal < Precedence.COLON.ordinal && input[i] == ':' -> {
                    i++
                    val temp = parseVal(input.substring(i), Precedence.COLON)
                    result.bits = temp.value.toInt()
                    if (i < input.length && input[i] == ':') {
                        i++
                        val temp2 = parseVal(input.substring(i), Precedence.COLON)
                        result.value = (result.value.toInt() ushr temp2.value.toInt()).toDouble()
                    }
                    if (result.bits < 0) {
                        result.bits = -result.bits
                        result.value = (reverse(result.value.toInt()) ushr (32 - result.bits)).toDouble()
                    }
                    result.value = (result.value.toInt() and config.mask[result.bits]).toDouble()
                }
                else -> break
            }
        }

        return result
    }

    private fun genHex(pattern: String): Pair<MutableList<Double>, Double> {
        val hex = mutableListOf<Double>()
        var cumulative = 0.0
        var pendingBits = if (msb) 1 else bitGroup

        fun addToHex(number: Double) {
            if (number == 0.0) return

            if (number > 0) {
                cumulative += number
                if (hex.size % 2 == 1) {
                    hex[hex.lastIndex] += number
                } else {
                    hex.add(number)
                }
            } else if (hex.isNotEmpty()) {
                cumulative -= number
                if (hex.size % 2 == 1) {
                    hex.add(-number)
                } else {
                    hex[hex.lastIndex] -= number
                }
            }
        }

        var i = 0
        while (i < pattern.length) {
            when (pattern[i]) {
                '*' -> {
                    val (newHex, newCumulative) = genHex(if (hex.isNotEmpty() && rPrefix != null) rPrefix!! else prefix!!)
                    hex.addAll(newHex)
                    cumulative += newCumulative
                    i++
                }
                '_' -> {
                    val (newHex, newCumulative) = genHex(if (hex.isNotEmpty() && rSuffix != null) rSuffix!! else suffix!!)
                    hex.addAll(newHex)
                    cumulative += newCumulative
                    i++
                    if (cumulative < messageTime) {
                        addToHex(cumulative - messageTime)
                    }
                }
                '^' -> {
                    i++
                    val value = parseVal(pattern.substring(i))
                    if (value.bits == 0) value.value *= timeBase
                    if (cumulative < value.value) {
                        addToHex(cumulative - value.value)
                    }
                    i = pattern.indexOfAny(charArrayOf(',', ';'), i).takeIf { it != -1 } ?: pattern.length
                }
                else -> {
                    val value = parseVal(pattern.substring(i))
                    if (value.bits == 0) value.value *= timeBase
                    if (value.bits <= 0) {
                        addToHex(value.value)
                    } else {
                        var number = value.value.toInt()
                        if (msb) number = reverse(number) ushr (32 - value.bits)
                        repeat(value.bits) {
                            if (msb) {
                                pendingBits = (pendingBits shl 1) + (number and 1)
                                if (pendingBits and bitGroup != 0) {
                                    val (newHex, newCumulative) = genHex(config.digits[pendingBits - bitGroup]!!)
                                    hex.addAll(newHex)
                                    cumulative += newCumulative
                                    pendingBits = 1
                                }
                            } else {
                                pendingBits = (pendingBits ushr 1) + (number and 1) * bitGroup
                                if (pendingBits and 1 != 0) {
                                    val (newHex, newCumulative) = genHex(config.digits[pendingBits ushr 1]!!)
                                    hex.addAll(newHex)
                                    cumulative += newCumulative
                                    pendingBits = bitGroup
                                }
                            }
                            number = number ushr 1
                        }
                    }
                    i = pattern.indexOfAny(charArrayOf(',', ';'), i).takeIf { it != -1 } ?: pattern.length
                }
            }

            if (i < pattern.length && pattern[i] == ';') {
                if (cumulative < messageTime) {
                    addToHex(cumulative - messageTime)
                }
                if (hex.size % 2 == 1) {
                    addToHex(-1.0)
                }
                cumulative = 0.0
            }
            i++
        }

        return Pair(hex, cumulative)
    }

    private fun getPair(result: IntArray, input: String) {
        var current = input
        for (nIndex in 0..1) {
            val num = current.takeWhile { it.isDigit() }.toIntOrNull() ?: break
            result[nIndex] = num
            current = current.dropWhile { it.isDigit() }
            if (current.length < 2 || current[0] != '.' || !current[1].isDigit()) break
            current = current.drop(1)
        }
    }


    fun generateRawData(): Triple<Int, Int, DoubleArray> {
        val (hex, cumulative) = genHex(form ?: "")
        if (cumulative < messageTime) {
            addToHex(hex, cumulative - messageTime)
        }
        if (hex.size % 2 == 1) {
            addToHex(hex, -1.0)
        }
        val single = hex.size / 2

        return Triple(single, hex.size / 2 - single, hex.toDoubleArray())
    }

    private fun addToHex(hex: MutableList<Double>, number: Double) {
        if (number == 0.0) return

        if (number > 0) {
            if (hex.size % 2 == 1) {
                hex[hex.lastIndex] += number
            } else {
                hex.add(number)
            }
        } else if (hex.isNotEmpty()) {
            if (hex.size % 2 == 1) {
                hex.add(-number)
            } else {
                hex[hex.lastIndex] -= number
            }
        }
    }
}

object IrEncoder {
    private const val TAG = "IrEncoder"

    private var protocolCache: MutableMap<String, String>? = null

    private fun getProtocolDefinitions(context: Context): Map<String, String> {
        synchronized(IrEncoder) {
            if (protocolCache != null) return protocolCache!!

            protocolCache = mutableMapOf()

            val allProtocolsKnown = context.assets.list("protocols")!!

            for (asset in allProtocolsKnown) {
                val protocol = asset.substringBeforeLast(".")
                val definition = context.assets.open("protocols/$asset").bufferedReader().use { it.readText() }
                Log.d(TAG, "Loaded protocol $protocol")
                protocolCache!![protocol.uppercase()] = definition
            }

            return protocolCache!!
        }
    }

    fun IRDBFunction.irpProtocolDefinition(context: Context): String? {
        val protocolDefinitions = getProtocolDefinitions(context)
        var protocolDef = protocolDefinitions[protocol.uppercase()]

        if (protocolDef == null) {
            // Protocol not found, try for special protocols
            val rc6Match = Regex("RC6-(\\d+)-(\\d+)").find(protocol.uppercase())
            if (rc6Match != null) {
                val (m, l) = rc6Match.destructured
                protocolDef = "Define M=$m\nDefine L=$l\n" + protocolDefinitions["RC6-M-L"]
            } else if (protocol.equals("NEC", ignoreCase = true)) {
                protocolDef = protocolDefinitions["NEC2"]
            } else if (protocol.equals("NECX", ignoreCase = true)) {
                protocolDef = protocolDefinitions["NECX2"]
            }
        }

        return protocolDef
    }

    fun String.frequency(): Double {
        return this.split(" ").filter { it.isNotEmpty() }.filter { it.uppercase().startsWith("FREQUENCY=") }.map { it.substringAfter("=").toDouble() }.firstOrNull() ?: 38400.0
    }

    /**
     * Encodes an IR signal based on the given protocol and parameters.
     *
     * @param protocol The name of the IR protocol to use for encoding.
     * @param device The device code for the IR signal.
     * @param subdevice The subdevice code for the IR signal (use -1 if not applicable).
     * @param function The function code for the IR signal.
     * @return A string containing the encoded IR signal as space-separated durations,
     *         or null if the protocol is not supported or an error occurs.
     */
    private fun IRDBFunction.calculateTimingString(context: Context): Pair<Double, DoubleArray>? {
        // Prepare the IRP string
        val irp = StringBuilder()

        // Handle D, S, F
        if (subdevice >= 0) {
            irp.append("Device=$device.$subdevice\nFunction=$function\n")
        } else {
            irp.append("Device=$device\nFunction=$function\n")
        }

        // Search for protocol
        val protocolDef = irpProtocolDefinition(context)
            ?: // Protocol not found, return null
            return null

        irp.append(protocolDef)

        // Encode
        val irpProcessor = IRP()

        try {
            if (!irpProcessor.readIrpString(irp.toString())) {
                throw IllegalArgumentException("Invalid IRP")
            }
        } catch (e: Exception) {
            // Invalid IRP, return null
            return null
        }

        // Generate the IR sequence
        irpProcessor.config.def['D' - 'A'] = device.toString()
        irpProcessor.config.def['S' - 'A'] =
            (if (subdevice != -1) subdevice else device.inv()).toString() // "CRC" for NEC
        irpProcessor.config.def['F' - 'A'] = function.toString()
        irpProcessor.config.def['N' - 'A'] = (-1).toString();
        val (_, _, raw) = irpProcessor.generateRawData()

        // Convert the sequence to a string
        return Pair(irpProcessor.frequency, raw)
    }

    private val timingStringCache = Collections.synchronizedMap(WeakHashMap<IRDBFunction, Pair<Double, DoubleArray>?>())

    fun IRDBFunction.timingString(context: Context) =
        timingStringCache.getOrPut(this) { calculateTimingString(context) }

    @OptIn(ExperimentalTime::class)
    fun IRDBFunction.transmissionLengthUs(context: Context): Double? {
        return when {
            protocol.uppercase().startsWith("NEC") -> Duration.convert(
                67.5,
                DurationUnit.MILLISECONDS,
                DurationUnit.MICROSECONDS
            )

            else -> timingString(context)?.second?.sum()
        }
    }

    fun IRDBFunction.transmissionLengthDuration(context: Context): Duration? {
        return transmissionLengthUs(context)?.toDuration(DurationUnit.MICROSECONDS)
    }
}

//https://techdocs.altium.com/display/FPGA/NEC+Infrared+Transmission+Protocol
@OptIn(ExperimentalTime::class)
val necRepeatPattern = intArrayOf(
    Duration.convert(9.0, DurationUnit.MILLISECONDS, DurationUnit.MICROSECONDS).toInt(),
    Duration.convert(2.25, DurationUnit.MILLISECONDS, DurationUnit.MICROSECONDS).toInt(),
    562.5.toInt()
)
