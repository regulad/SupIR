package xyz.regulad.supir.ir

import android.content.Context
import android.os.Build
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import xyz.regulad.regulib.FlowCache.Companion.asCached
import xyz.regulad.regulib.merge
import xyz.regulad.supir.ir.providers.loadAllFlipperBrands
import xyz.regulad.supir.ir.providers.loadAllIrdbBrands

fun loadAllBrands(context: Context): Flow<SBrand> {
    val ephemeralMergedFlow = listOf(
        loadAllIrdbBrands(context),
        loadAllFlipperBrands(context)
    )
        .merge()
//        .asCached(context, null)

    val versionNumber = context.packageManager.getPackageInfo(context.packageName, 0).versionCode

    // realistically, there's no reason for this to be a flow. however, it makes the rest of the code easier to write
    return flow {
        val brands = mutableListOf<SBrand>()
        val brandEditMutex = Mutex()

        ephemeralMergedFlow.collect { brand ->
            brandEditMutex.withLock {
                val existingBrand = brands.find { it.name == brand.name }

                if (existingBrand != null) {
                    brands.remove(existingBrand)
                    brands.add(existingBrand.merge(brand))
                } else {
                    brands.add(brand)
                }
            }
        }

        // pr needed: make these emit in real time
        brands.sortBy { it.name }
        brands.forEach { emit(it) }
    }
        .asCached(context, "${Build.BOARD}+merge+$versionNumber")
}
