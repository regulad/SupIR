package xyz.regulad.supir.ir

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class IRDBFunction(
    val functionName: String,
    val protocol: String,
    val device: Int,
    val subdevice: Int,
    val function: Int,
) : Parcelable
