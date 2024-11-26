package xyz.regulad.supir.ir.providers

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import xyz.regulad.supir.ir.*
import xyz.regulad.supir.ir.TransmitterManager.isTransmittable

private const val TAG = "FlipperIRDBLoader"

// "00 01 0a 0b"
// lsb -> msb
fun parseHexInt(inputString: String): Int {
    val bytes = inputString.split(" ")
        .map { it.toUByte(16) }

    var accumulator = 0
    var shift = 0

    for (byte in bytes) {
        // Convert signed byte to unsigned int (0-255)
        val unsignedByte = byte.toInt() and 0xFF
        // Shift the byte to its position and OR it with accumulator
        accumulator = accumulator or (unsignedByte shl shift)
        shift += 8
    }

    return accumulator
}

/**
 * Loads an IR file from the assets directory.
 *
 * Returns a triple:
 * - The brand name
 * - The category name
 * - SModel
 */
fun loadIrFile(context: Context, path: String): Triple<String, String, SModel> {
    Log.d(TAG, "Loading Flipper IR file $path")

    val irData = context.assets.open(path).use {
        it
            .reader(Charsets.UTF_8)
            .readText()
    }

    val pathComponents = path.split("/")

    val brandName = pathComponents[1].replace('_', ' ')
    val categoryName = pathComponents[2].replace('_', ' ')
    val modelIdentifier = pathComponents[3].removeSuffix(".ir").replace('_', ' ')

    val functions = mutableListOf<IRFunction>()

    // step through the lines of the IR file
    val irIterator = irData.lineSequence().iterator()

    fun takeLine(): String {
        return irIterator.next().substringBefore('#').trim()
    }

    while (irIterator.hasNext()) {
        val line = takeLine()

        if (line.isBlank()) {
            continue
        }

        when {
            line.startsWith("Filetype:", ignoreCase = true) -> {
                if (!line.equals("Filetype: IR signals file", ignoreCase = true)) {
                    throw IllegalArgumentException("Unsupported filetype: $line")
                }
            }

            line.startsWith("Version:", ignoreCase = true) -> {
                if (!line.equals("Version: 1", ignoreCase = true)) {
                    throw IllegalArgumentException("Unsupported version: $line")
                }
            }

            line.startsWith("name:", ignoreCase = true) -> {
                val name = line.substring("name: ".length).trim().replace('_', ' ')

                val maybeTypeLine = takeLine()

                val type = if (maybeTypeLine.startsWith("type:", ignoreCase = true)) {
                    maybeTypeLine.substring("type: ".length).trim()
                } else {
                    throw IllegalArgumentException("Expected Type: line, got $maybeTypeLine")
                }

                when (type) {
                    "parsed" -> {
                        val protocolLine = takeLine()
                        val addressLine = takeLine()
                        val commandLine = takeLine()

                        val protocol = if (protocolLine.startsWith("protocol:", ignoreCase = true)) {
                            protocolLine.substring("protocol: ".length).trim()
                        } else {
                            throw IllegalArgumentException("Expected Protocol: line, got $protocolLine")
                        }

                        val address = if (addressLine.startsWith("address:", ignoreCase = true)) {
                            parseHexInt(addressLine.substring("address: ".length).trim())
                        } else {
                            throw IllegalArgumentException("Expected Address: line, got $addressLine")
                        }

                        val command = if (commandLine.startsWith("command:", ignoreCase = true)) {
                            parseHexInt(commandLine.substring("command: ".length).trim())
                        } else {
                            throw IllegalArgumentException("Expected Command: line, got $commandLine")
                        }

                        // necext is a special half-16bit half-8bit protocol
                        if (protocol.equals("NECext", ignoreCase = true)) {
                            // special handling
                            val commandMsb = command and 0x000000FF

                            val upperAddress = address and 0x000000FF
                            val lowerAddress = (address and 0x0000FF00) shr 8

                            functions.add(IRFunction(name, "nec", upperAddress, lowerAddress, commandMsb))
                        } else {
                            functions.add(IRFunction(name, protocol, address, null, command))
                        }
                    }

                    "raw" -> {
                        val frequencyLine = takeLine()
                        val dutyCycleLine = takeLine()
                        val dataLine = takeLine()

                        val frequency = if (frequencyLine.startsWith("frequency:", ignoreCase = true)) {
                            frequencyLine.substring("frequency: ".length).trim().toInt()
                        } else {
                            throw IllegalArgumentException("Expected Frequency: line, got $frequencyLine")
                        }

                        val dutyCycle = if (dutyCycleLine.startsWith("duty_cycle:", ignoreCase = true)) {
                            dutyCycleLine.substring("duty cycle: ".length).trim().toDouble()
                        } else {
                            throw IllegalArgumentException("Expected Duty Cycle: line, got $dutyCycleLine")
                        }

                        val data = if (dataLine.startsWith("data:", ignoreCase = true)) {
                            dataLine.substring("data: ".length).trim().split(" ").map { it.toInt(10) }
                        } else {
                            throw IllegalArgumentException("Expected Data: line, got $dataLine")
                        }

                        val rawData = RawData(frequency, dutyCycle, data)
                        functions.add(IRFunction(name, null, null, null, null, rawData))
                    }

                    else -> {
                        throw IllegalArgumentException("Unsupported type: $type")
                    }
                }
            }
        }
    }

    return Triple(brandName, categoryName, SModel(modelIdentifier, functions.filter { it.isTransmittable(context) }))
}

internal fun loadAllFlipperBrands(context: Context): Flow<SBrand> {
    Log.d(TAG, "Loading all Flipper brands")

    return channelFlow {
        coroutineScope {
            launch(Dispatchers.IO) {
                val brands = context.assets
                    .list("flipper")!!
                    .asSequence()
                    .map { category ->
                        "flipper/$category"
                    }
                    .flatMap { categoryPath ->
                        context.assets.list(categoryPath)!!.map { model ->
                            "$categoryPath/$model"
                        }
                    }
                    .flatMap { brandPath ->
                        context.assets.list(brandPath)!!.map { model ->
                            "$brandPath/$model"
                        }
                    }
                    .filter { it.endsWith(".ir") }
                    .asFlow()
                    .mapNotNull { path ->
                        try {
                            loadIrFile(context, path)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to load $path", e)
                            null
                        }
                    }
                    .filter { (_, _, sModel) -> sModel.functions.isNotEmpty() }
                    .fold(emptyList<SBrand>()) { acc, (brandName, category, sModel) ->
                        val existingBrand = acc.find { it.name == brandName }

                        if (existingBrand != null) {
                            (acc - existingBrand) + existingBrand.merge(
                                SBrand(
                                    brandName,
                                    listOf(SCategory(category, listOf(sModel)))
                                )
                            )
                        } else {
                            acc + SBrand(brandName, listOf(SCategory(category, listOf(sModel))))
                        }
                    }

                brands.forEach { channel.send(it) }
            }
        }
    }
}
