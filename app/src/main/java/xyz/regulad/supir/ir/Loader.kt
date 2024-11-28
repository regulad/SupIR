package xyz.regulad.supir.ir

import android.content.Context
import android.os.Build
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import xyz.regulad.regulib.FlowCache.Companion.asCached
import xyz.regulad.regulib.merge
import xyz.regulad.supir.SupIRPreferencesRepository
import xyz.regulad.supir.ir.providers.loadAllFlipperBrands
import xyz.regulad.supir.ir.providers.loadAllIrdbBrands
import xyz.regulad.supir.ir.providers.loadAllIrextBrands

fun loadAllBrands(context: Context, preferences: SupIRPreferencesRepository): Flow<SBrand> {
    val loadIrdb = preferences.loadIrdb
    val loadFlipper = preferences.loadFlipper
    val loadIrext = preferences.loadIrext

    val allBackendFlows = listOfNotNull(
        if (loadIrdb) loadAllIrdbBrands(context) else null,
        if (loadFlipper) loadAllFlipperBrands(context) else null,
        if (loadIrext) loadAllIrextBrands(context) else null
    )

    val ephemeralMergedFlow = allBackendFlows
        .merge()

    val flowIdent = "irdb:${loadIrdb}++flipper:${loadFlipper}++irext:${loadIrext}"

    val versionNumber = context.packageManager.getPackageInfo(context.packageName, 0).versionCode

    val outputFlow = if (allBackendFlows.size > 1) {
        flow {
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
    } else {
        ephemeralMergedFlow
    }

    // realistically, there's no reason for this to be a flow. however, it makes the rest of the code easier to write
    return outputFlow.asCached(context, "${Build.BOARD}+$flowIdent+$versionNumber")
}
