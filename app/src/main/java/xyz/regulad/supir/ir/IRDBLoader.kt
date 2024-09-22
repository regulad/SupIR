package xyz.regulad.supir.ir

import android.content.res.AssetManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOn
import xyz.regulad.makehex.IRDBFunction
import xyz.regulad.supir.util.FlowCache.cached
import java.io.InputStream

private const val TAG = "IRDBLoader"

data class IndexEntry(val brandName: String, val modelCategory: String, val fileName: String)

data class Brand(private val assetManager: AssetManager, val name: String, private val indexEntries: List<IndexEntry>) {
    val categories by lazy {
        indexEntries
            .groupBy { it.modelCategory }
            .map { (category, entries) -> Category(assetManager, name, category, entries) }
            .filter { it.models.any() }
            .sortedBy { it.name }
    }
}

data class Category(private val assetManager: AssetManager, private val brandName: String, val name: String, private val indexEntries: List<IndexEntry>) {
    val models by lazy {
        indexEntries
            .map { Model(assetManager, brandName, name, it.fileName) }
            .filter { it.functions.any() }
            .sortedBy { it.identifier }
    }
}

data class Model(private val assetManager: AssetManager, private val brandName: String, val category: String, val identifier: String) {
    companion object {
        private val functionMap = mutableMapOf<Triple<String, String, String>, List<IRDBFunction>>()
    }

    val functions by lazy {
        functionMap.getOrPut(Triple(brandName, category, identifier)) {
            val csvInputStream: InputStream = try {
                assetManager.open("codes/$brandName.$category/$identifier.csv")
            } catch (e: Exception) {
                try {
                    assetManager.open("codes/$brandName/$category/$identifier.csv")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load $brandName/$category/$identifier.csv")
                    return@lazy emptyList<IRDBFunction>()
                }
            }

            Log.d(TAG, "Loading $brandName/$category/$identifier.csv")

            csvInputStream.reader()
                .readLines()
                .drop(1) // Skip header
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    val parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex()).map { it.trim('"') }
                    if (parts.size != 5) {
                        Log.e(TAG, "Invalid line: $line")
                        null
                    } else {
                        val (functionName, protocol, device, subdevice, function) = parts
                        IRDBFunction(
                            functionName,
                            protocol,
                            device.toInt(),
                            subdevice.toInt(),
                            function.toInt()
                        )
                    }
                }
                .filter { it.transmittable }
                .toList()
        }
    }
}

fun loadAllBrands(assetManager: AssetManager): Flow<Brand> {
    Log.d(TAG, "Loading IRDB Index")

    return assetManager.open("codes/index")
        .reader()
        .readLines()
        .asSequence()
        .map {
            val (brandName, modelCategory, fileName) = it.split("/")
            IndexEntry(brandName, modelCategory, fileName.removeSuffix(".csv"))
        }
        .groupBy { it.brandName } // terminal
        .asSequence()
        .sortedBy { it.key } // from here on, do things on demand
        .map { (brandName, entries) -> Brand(assetManager, brandName, entries) }
        .filter { it.categories.any() }
        .asFlow()
        .flowOn(Dispatchers.IO)
        .cached()
}
