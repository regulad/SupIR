package xyz.regulad.supir.ir.providers

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOn
import xyz.regulad.supir.ir.IRFunction
import xyz.regulad.supir.ir.SBrand
import xyz.regulad.supir.ir.SCategory
import xyz.regulad.supir.ir.SModel
import xyz.regulad.supir.ir.TransmitterManager.isTransmittable
import java.io.InputStream

private const val TAG = "IRDBLoader"

private data class IrdbIndexEntry(val brandName: String, val modelCategory: String, val fileName: String)

private data class IrdbBrand(
    private val context: Context,
    val name: String,
    private val indexEntries: List<IrdbIndexEntry>
) {
    val categories by lazy {
        indexEntries
            .groupBy { it.modelCategory }
            .map { (category, entries) -> IrdbCategory(context, name, category, entries) }
            .filter { it.models.any() }
            .sortedBy { it.name }
    }

    val sBrand by lazy {
        SBrand(name, categories.map { it.sCategory })
    }
}

private data class IrdbCategory(
    private val context: Context,
    private val brandName: String,
    val name: String,
    private val indexEntries: List<IrdbIndexEntry>
) {
    val models by lazy {
        indexEntries
            .map { IrdbModel(context, brandName, name, it.fileName) }
            .filter { it.functions.any() }
            .sortedBy { it.identifier }
    }

    val sCategory by lazy {
        SCategory(name, models.map { it.sModel })
    }
}


private data class IrdbModel(
    private val context: Context,
    private val brandName: String,
    val category: String,
    val identifier: String
) {
    val sModel by lazy {
        SModel(identifier, functions)
    }

    companion object {
        private val functionMap = mutableMapOf<Triple<String, String, String>, List<IRFunction>>()
    }

    val functions by lazy {
        functionMap.getOrPut(Triple(brandName, category, identifier)) {
            val csvInputStream: InputStream = try {
                context.assets.open("codes/$brandName.$category/$identifier.csv")
            } catch (e: Exception) {
                try {
                    context.assets.open("codes/$brandName/$category/$identifier.csv")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load $brandName/$category/$identifier.csv")
                    return@lazy emptyList<IRFunction>()
                }
            }

            Log.d(TAG, "Loading $brandName/$category/$identifier.csv")

            csvInputStream.use {
                it
                    .reader(Charsets.UTF_8)
                    .readLines()
            }
                .drop(1) // Skip header
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    val parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex()).map { it.trim('"') }
                    if (parts.size != 5) {
                        Log.e(TAG, "Invalid line: $line")
                        null
                    } else {
                        val (functionName, protocol, device, subdevice, function) = parts
                        IRFunction(
                            functionName,
                            protocol,
                            device.toInt(),
                            subdevice.toInt(),
                            function.toInt()
                        )
                    }
                }
                .filter { it.isTransmittable(context) }
                .toList()
        }
    }
}

internal fun loadAllIrdbBrands(context: Context): Flow<SBrand> {
    Log.d(TAG, "Loading IRDB Index")

    return context.assets.open("codes/index")
        .reader()
        .readLines()
        .asSequence()
        .map {
            val (brandName, modelCategory, fileName) = it.split("/")
            IrdbIndexEntry(brandName, modelCategory, fileName.removeSuffix(".csv"))
        }
        .groupBy { it.brandName } // terminal
        .asSequence()
        .sortedBy { it.key } // from here on, do things on demand
        .map { (brandName, entries) -> IrdbBrand(context, brandName, entries) }
        .filter { it.categories.any() }
        .map { it.sBrand }
        .asFlow()
        .flowOn(Dispatchers.IO)
}
