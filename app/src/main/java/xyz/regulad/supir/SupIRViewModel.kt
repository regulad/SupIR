package xyz.regulad.supir

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.obd.infrared.transmit.Transmitter
import kotlinx.coroutines.flow.MutableStateFlow
import xyz.regulad.supir.ir.loadAllBrands

class SupIRViewModel(application: Application) : AndroidViewModel(application) {
    val transmitter: Transmitter? = Transmitter.getTransmitterForDevice(application)
    val allBrandsFlow = loadAllBrands(application)
    val preferencesRepository = SupIRPreferencesRepository(application)

    private var _favoriteBrandModels = MutableStateFlow(preferencesRepository.favoriteBrandModels)
    val favoriteBrandModels = _favoriteBrandModels

    fun setBrandModelFavorite(brandModel: String, isFavorite: Boolean) {
        val newFavoriteBrandModels = if (isFavorite) {
            _favoriteBrandModels.value + brandModel
        } else {
            _favoriteBrandModels.value - brandModel
        }
        _favoriteBrandModels.value = newFavoriteBrandModels
        preferencesRepository.favoriteBrandModels = newFavoriteBrandModels
    }
}
