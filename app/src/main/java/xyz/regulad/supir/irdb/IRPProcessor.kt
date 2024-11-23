package xyz.regulad.supir.irdb

import android.content.Context
import android.util.Log
import kotlin.experimental.inv

/**
 * IRP class for handling Infrared Remote Protocol
 * http://www.hifi-remote.com/wiki/index.php/IRP_Notation
 *
 * Heavily inspired by [MakeHex's implementation of IRP](https://github.com/probonopd/MakeHex)
 *
 * I, Parker Wahle, do not understand most of how this class works. Please add documentation if you understand!
 */
class IRPProcessor {
    var frequency: Double = 0.0 // without a frequency line, assume each is baseband
    var timeBase: Double = 1.0
    var messageTime: Double = 0.0
    var isMsb: Boolean = false
    private var bitGroup: Int = 2

    private var prefix: String? = null
    private var suffix: String? = null
    private var repeatPrefix: String? = null
    private var repeatSuffix: String? = null
    private var form: String? = null

    val canRepeat: Boolean
        get() = form != null && ";" in form!!

    private val initialForm: String?
        get() = form?.substringBefore(";")

    private val repeatForm: String?
        get() = form?.substringAfter(";", "")

    companion object {
        enum class Precedence { UNARY, TIMES, PLUS, COLON }
        data class Value(var value: Double, var bits: Int)

        /**
         * Parses a value that may be interspersed with letters and letter into a Value tuple
         */
        private fun parseVal(input: String, config: IRPConfig, prec: Precedence = Precedence.UNARY): Value {
            var result = Value(0.0, 0)
            var i = 0

            when {
                input[i] in 'A'..'Z' -> {
                    val letter = input[i]
                    i++

                    config.getLetterDefinition(letter)?.let { result = parseVal(it, config) } ?: run {
                        result.value = config.getLetterValue(letter).toDouble()
                    }
                }

                input[i] in '0'..'9' -> {
                    result.value = input.substring(i).takeWhile { it.isDigit() }.toDouble()
                    i += result.value.toString().length
                }

                input[i] == '-' -> {
                    i++
                    val temp = parseVal(input.substring(i), config, Precedence.UNARY)
                    result.value = -temp.value
                    result.bits = if (temp.bits > 0) 0 else temp.bits
                }

                input[i] == '~' -> {
                    i++
                    val temp = parseVal(input.substring(i), config, Precedence.UNARY)
                    result.value = temp.value.toInt().toByte().inv().toDouble()
                    if (temp.bits > 0) {
                        result.value = (result.value.toInt() and config.mask[temp.bits]).toDouble()
                        result.bits = temp.bits
                    }
                }

                input[i] == '(' -> {
                    val closingIndex = input.indexOf(')', i)
                    if (closingIndex == -1) throw IllegalArgumentException("Mismatched parentheses")
                    result = parseVal(input.substring(i + 1, closingIndex), config)
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
                        val temp = parseVal(input.substring(i), config, Precedence.TIMES)
                        result.value *= temp.value
                        if (result.bits > 0) result.bits = 0
                    }

                    prec.ordinal < Precedence.PLUS.ordinal && input[i] in setOf('+', '-', '^') -> {
                        val op = input[i]
                        i++
                        val temp = parseVal(input.substring(i), config, Precedence.PLUS)
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
                        val temp = parseVal(input.substring(i), config, Precedence.COLON)
                        result.bits = temp.value.toInt()
                        if (i < input.length && input[i] == ':') {
                            i++
                            val temp2 = parseVal(input.substring(i), config, Precedence.COLON)
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

        // this is not a data class, its very mutable
        class IRPConfig {
            val digits: Array<String?> = arrayOfNulls(16)
            private val definitions: Array<String?> = arrayOfNulls(26)
            private val values: IntArray = IntArray(26)
            val device: IntArray = intArrayOf(-1, -1)
            val functions: IntArray = intArrayOf(-1, -1, -1, -1)
            val mask: IntArray = createMask()

            /**
             * The IRP notation revolves around setting letters equal to given values, this is a helper function for that task.
             */
            fun setLetterDefinition(letter: Char, value: String?) {
                definitions[letter.uppercase().toCharArray().first() - 'A'] = value
            }

            fun getLetterDefinition(letter: Char) =
                definitions[letter.uppercase().toCharArray().first() - 'A']

            fun setLetterValue(letter: Char, value: Int) {
                values[letter.uppercase().toCharArray().first() - 'A'] = value
            }

            fun getLetterValue(letter: Char) =
                values[letter.uppercase().toCharArray().first() - 'A']
        }

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

        private fun addToHexList(hexList: MutableList<Double>, number: Double) {
            if (number == 0.0) return

            if (number > 0) {
                if (hexList.size % 2 == 1) {
                    hexList[hexList.lastIndex] += number
                } else {
                    hexList.add(number)
                }
            } else if (hexList.isNotEmpty()) {
                if (hexList.size % 2 == 1) {
                    hexList.add(-number)
                } else {
                    hexList[hexList.lastIndex] -= number
                }
            }
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
    }

    internal val config = IRPConfig()

    fun readIrpString(str: String): Boolean {
        str.lines().map { it.uppercase().substringBefore("'").trim() }.filter { it.isNotEmpty() }
            .forEach { line ->
                try {
                    processLine(line)
                } catch (e: Exception) {
                    Log.e("IRP", "Error processing line: \"$line\"", e)
                }
            }

        return form != null &&
                config.digits[0] != null &&
                config.digits[1] != null &&
                config.functions[0] != -1 &&
                !(config.functions[2] >= 0 && config.functions[2] != config.functions[0] && config.functions[3] != config.functions[1])
    }

    private fun processLine(line: String) {
        val nameValueList = line.split("=")
        val (name, value) = Pair(nameValueList[0], nameValueList[1])

        when {
            name == "FREQUENCY" -> {
                frequency = parseVal(value, config).value
            }

            name == "TIME BASE" -> {
                timeBase = parseVal(value, config).value
            }

            name == "MESSAGE TIME" -> {
                val parsed = parseVal(value, config)
                messageTime = if (parsed.bits == 0) parsed.value * timeBase else parsed.value
            }

            // numbers
            name.toIntOrNull() != null -> {
                setDigit(name.toInt(), value)
            }
            // pre-cooked number shorthands (rare but valid)
            name == "ZERO" -> {
                setDigit(0, value)
            }

            name == "ONE" -> {
                setDigit(1, value)
            }

            name == "TWO" -> {
                setDigit(2, value)
            }

            name == "THREE" -> {
                setDigit(3, value)
            }

            name == "FOUR" -> {
                setDigit(4, value)
            }

            name == "FIVE" -> {
                setDigit(5, value)
            }

            name == "SIX" -> {
                setDigit(6, value)
            }

            name == "SEVEN" -> {
                setDigit(7, value)
            }

            name == "EIGHT" -> {
                setDigit(8, value)
            }

            name == "NINE" -> {
                setDigit(9, value)
            }

            name == "TEN" -> {
                setDigit(10, value)
            }

            name == "ELEVEN" -> {
                setDigit(11, value)
            }

            name == "TWELVE" -> {
                setDigit(12, value)
            }

            name == "THIRTEEN" -> {
                setDigit(13, value)
            }

            name == "FOURTEEN" -> {
                setDigit(14, value)
            }

            name == "FIFTEEN" -> {
                setDigit(15, value)
            }

            // forms, prefixes/suffixes, and repeated prefix/suffix
            name == "PREFIX" -> {
                prefix = value
            }

            name == "SUFFIX" -> {
                suffix = value
            }

            name == "R-PREFIX" -> {
                repeatPrefix = value
            }

            name == "R-SUFFIX" -> {
                repeatSuffix = value
            }

            name == "FORM" -> {
                form = value
            }

            // msb
            name == "FIRST BIT" -> {
                isMsb = value.equals("MSB", ignoreCase = true)
            }

            name.startsWith("DEFINE") || line.startsWith("DEFAULT") -> {
                val letterBeingDefined = name.last()
                config.setLetterDefinition(letterBeingDefined, value)
            }

            // devices/functions
            name == "DEVICE" -> {
                getPair(config.device, value)
            }

            name == "FUNCTION" -> {
                getPair(config.functions, value)
                if (value.startsWith("..")) {
                    getPair(config.functions.copyOfRange(2, 4), value.substring(2))
                }
            }
        }
    }

    private fun setDigit(d: Int, value: String) {
        config.digits[d] = value
        while (d >= bitGroup) bitGroup = bitGroup shl 1
    }

    /**
     * Generates the hex value for a given form as a list of doubles and the message time.
     *
     * @param isRepeat repeats follow a different set of rules
     */
    private fun genHex(pattern: String, isRepeat: Boolean = false): Pair<MutableList<Double>, Double> {
        val hex = mutableListOf<Double>()
        var cumulative = 0.0
        var pendingBits = if (isMsb) 1 else bitGroup

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
                // for repeats, when a repeatPrefix is not defined, the regular prefix is used isntead

                // prefix
                '*' -> {
                    val (newHex, newCumulative) = genHex((if (isRepeat) repeatPrefix ?: prefix else prefix) ?: "")
                    hex.addAll(newHex)
                    cumulative += newCumulative
                    i++
                }

                // suffix
                '_' -> {
                    val (newHex, newCumulative) = genHex((if (isRepeat) repeatSuffix ?: suffix else suffix) ?: "")
                    hex.addAll(newHex)
                    cumulative += newCumulative
                    i++
                    if (cumulative < messageTime) {
                        addToHex(cumulative - messageTime)
                    }
                }

                '^' -> {
                    i++
                    val value = parseVal(pattern.substring(i), config)
                    if (value.bits == 0) value.value *= timeBase
                    if (cumulative < value.value) {
                        addToHex(cumulative - value.value)
                    }
                    // seek to next command or end of string
                    i = pattern.indexOf(',', i).takeIf { it != -1 } ?: pattern.length
                }

                else -> {
                    val value = parseVal(pattern.substring(i), config)
                    if (value.bits == 0) value.value *= timeBase
                    if (value.bits <= 0) {
                        addToHex(value.value)
                    } else {
                        var number = value.value.toInt()
                        if (isMsb) number = reverse(number) ushr (32 - value.bits)
                        repeat(value.bits) {
                            if (isMsb) {
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
                    // seek to next command or end of string
                    i = pattern.indexOf(',', i).takeIf { it != -1 } ?: pattern.length
                }
            }

            i++
        }

        return Pair(hex, cumulative)
    }


    /**
     * Generates the raw data for the IRPProcessor
     *
     * @param isRepeat whether the data is a repeat
     */
    fun generateRawData(isRepeat: Boolean = false): DoubleArray {
        val (hex, cumulative) = genHex((if (isRepeat) repeatForm else initialForm) ?: "", isRepeat = isRepeat)

        if (!isRepeat) {
            if (cumulative < messageTime) {
                addToHexList(hex, cumulative - messageTime)
            }

            if (hex.size % 2 == 1) {
                addToHexList(hex, -1.0)
            }
        }

        return hex.toDoubleArray()
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

    private fun IRDBFunction.irpProtocolDefinition(context: Context): String? {
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

    fun IRDBFunction.getIrpProcessor(context: Context): IRPProcessor? {
        // Prepare the IRP string
        val irp = StringBuilder()

        // Search for protocol
        val protocolDef = irpProtocolDefinition(context)
            ?: // Protocol not found, return null
            return null

        irp.append(protocolDef)

        // Encode
        val irpProcessor = IRPProcessor()

        try {
            if (!irpProcessor.readIrpString(irp.toString())) {
                throw IllegalArgumentException("Invalid IRP")
            }
        } catch (e: Exception) {
            // Invalid IRP, return null
            return null
        }

        // Generate the IR sequence
        irpProcessor.config.setLetterDefinition('D', device.toString())
        if (subdevice != -1) {
            irpProcessor.config.setLetterDefinition('S', subdevice.toString())
        }
        irpProcessor.config.setLetterDefinition('F', function.toString())

        return irpProcessor
    }

    fun IRDBFunction.getFrequency(context: Context): Double? {
        val irpProcessor = getIrpProcessor(context) ?: return null
        return irpProcessor.frequency
    }

    fun IRDBFunction.initialTimingString(context: Context): Pair<Double, DoubleArray>? {
        val irpProcessor = getIrpProcessor(context) ?: return null

        val raw = irpProcessor.generateRawData(false)

        // Convert the sequence to a string
        return Pair(irpProcessor.frequency, raw)
    }

    fun IRDBFunction.repeatTimingString(context: Context): Pair<Double, DoubleArray>? {
        val irpProcessor = getIrpProcessor(context) ?: return null

        val raw = irpProcessor.generateRawData(true)

        // Convert the sequence to a string
        return Pair(irpProcessor.frequency, raw)
    }
}
