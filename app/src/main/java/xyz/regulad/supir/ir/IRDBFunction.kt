package xyz.regulad.supir.ir

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class IRDBFunction(
    val functionName: String,
    val protocol: String,
    val device: Int,
    val subdevice: Int,
    val function: Int,
) : Parcelable {
    val identifier: String get() {
        return "'$functionName','$protocol','$device','$subdevice','$function'"
    }
}
