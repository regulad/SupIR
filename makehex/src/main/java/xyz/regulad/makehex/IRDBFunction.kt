package xyz.regulad.makehex

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class IRDBFunction(
    val functionName: String,
    val protocol: String,
    val device: Int,
    val subdevice: Int,
    val function: Int,
) : Parcelable {
    fun timingString(): String? {
        return MakeHex().encodeIr(protocol, device, subdevice, function)
    }
}
