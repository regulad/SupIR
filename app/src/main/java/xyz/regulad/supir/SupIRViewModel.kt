package xyz.regulad.supir

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.obd.infrared.transmit.Transmitter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import xyz.regulad.supir.ir.loadAllBrands

class SupIRViewModel(application: Application) : AndroidViewModel(application) {
    val transmitter: Transmitter? = Transmitter.getTransmitterForDevice(application)
    private val preferencesRepository = SupIRPreferencesRepository(application)

    private var _favoriteBrandModels = MutableStateFlow(preferencesRepository.favoriteBrandModels)
    val favoriteBrandModels = _favoriteBrandModels.asStateFlow()

    fun setBrandModelFavorite(brandModel: String, isFavorite: Boolean) {
        val newFavoriteBrandModels = if (isFavorite) {
            _favoriteBrandModels.value + brandModel
        } else {
            _favoriteBrandModels.value - brandModel
        }
        _favoriteBrandModels.value = newFavoriteBrandModels
        preferencesRepository.favoriteBrandModels = newFavoriteBrandModels
    }

    private var _loadIrdb = MutableStateFlow(preferencesRepository.loadIrdb)
    val loadIrdb = _loadIrdb.asStateFlow()

    fun setLoadIrdb(loadIrdb: Boolean) {
        _loadIrdb.value = loadIrdb
        preferencesRepository.loadIrdb = loadIrdb
    }

    private var _loadFlipper = MutableStateFlow(preferencesRepository.loadFlipper)
    val loadFlipper = _loadFlipper.asStateFlow()

    fun setLoadFlipper(loadFlipper: Boolean) {
        _loadFlipper.value = loadFlipper
        preferencesRepository.loadFlipper = loadFlipper
    }

    private var _loadIrext = MutableStateFlow(preferencesRepository.loadIrext)
    val loadIrext = _loadIrext.asStateFlow()

    fun setLoadIrext(loadIrext: Boolean) {
        _loadIrext.value = loadIrext
        preferencesRepository.loadIrext = loadIrext
    }

    val allBrandsFlow = loadAllBrands(application, preferencesRepository)
}
