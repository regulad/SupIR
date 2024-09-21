package xyz.regulad.supir.ui.nav

import android.content.Context
import android.widget.Toast

fun Context.showToast(message: String) {
    Toast.makeText(this@showToast, message, Toast.LENGTH_LONG).show()
}
