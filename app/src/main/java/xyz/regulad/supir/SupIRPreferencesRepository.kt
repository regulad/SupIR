package xyz.regulad.supir

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SupIRPreferencesRepository(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "user_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val FAVORITE_BRAND_MODELS = "favorite_brand_models"

        private const val LOAD_IRDB = "load_irdb"
        private const val LOAD_FLIPPER = "load_flipper"
        private const val LOAD_IREXT = "load_irext"
    }

    var favoriteBrandModels: Set<String>
        get() = sharedPreferences.getStringSet(FAVORITE_BRAND_MODELS, emptySet()) ?: emptySet()
        set(value) = sharedPreferences.edit().putStringSet(FAVORITE_BRAND_MODELS, value).apply()

    var loadIrdb: Boolean
        get() = sharedPreferences.getBoolean(LOAD_IRDB, true)
        set(value) = sharedPreferences.edit().putBoolean(LOAD_IRDB, value).apply()

    var loadFlipper: Boolean
        get() = sharedPreferences.getBoolean(LOAD_FLIPPER, false)
        set(value) = sharedPreferences.edit().putBoolean(LOAD_FLIPPER, value).apply()

    var loadIrext: Boolean
        get() = sharedPreferences.getBoolean(LOAD_IREXT, false)
        set(value) = sharedPreferences.edit().putBoolean(LOAD_IREXT, value).apply()
}
