package xyz.regulad.supir.ir

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable

@Serializable
data class IRDBFunction(
    val functionName: String,
    val protocol: String,
    val device: Int,
    val subdevice: Int,
    val function: Int,
) {
    val identifier: String get() {
        return "'$functionName','$protocol','$device','$subdevice','$function'"
    }

    val icon: ImageVector
        get() {
            val lowerFunctionName = functionName.lowercase()
            return when {
                "power" in lowerFunctionName -> Icons.Filled.Power
                "on" in lowerFunctionName -> Icons.Filled.Power
                "off" in lowerFunctionName -> Icons.Filled.PowerOff
                "bluetooth" in lowerFunctionName -> Icons.Filled.Bluetooth
                "settings" in lowerFunctionName -> Icons.Filled.Settings
                "play" in lowerFunctionName -> Icons.Filled.PlayArrow
                "pause" in lowerFunctionName -> Icons.Filled.Pause
                "stop" in lowerFunctionName -> Icons.Filled.Stop
                "record" in lowerFunctionName -> Icons.Filled.FiberManualRecord
                "disc" in lowerFunctionName -> Icons.Filled.DiscFull
                "rewind" in lowerFunctionName || "rev" in lowerFunctionName -> Icons.Filled.FastRewind
                "forward" in lowerFunctionName || "ff" in lowerFunctionName -> Icons.Filled.FastForward
                "up" in lowerFunctionName -> Icons.Filled.ArrowUpward
                "down" in lowerFunctionName || "dn" in lowerFunctionName -> Icons.Filled.ArrowDownward
                "left" in lowerFunctionName -> Icons.AutoMirrored.Filled.ArrowBack
                "right" in lowerFunctionName -> Icons.AutoMirrored.Filled.ArrowForward
                "fav" in lowerFunctionName -> Icons.Filled.Favorite
                "guide" in lowerFunctionName -> Icons.Filled.Tv
                "tv" in lowerFunctionName -> Icons.Filled.Tv
                "info" in lowerFunctionName -> Icons.Filled.Info
                "menu" in lowerFunctionName -> Icons.Filled.Menu
                "prev" in lowerFunctionName -> Icons.Filled.SkipPrevious
                "next" in lowerFunctionName -> Icons.Filled.SkipNext
                "hdmi" in lowerFunctionName -> Icons.Filled.SettingsInputHdmi
                "component" in lowerFunctionName -> Icons.Filled.SettingsInputComponent
                "composite" in lowerFunctionName -> Icons.Filled.SettingsInputComposite
                "s-video" in lowerFunctionName || "svideo" in lowerFunctionName -> Icons.Filled.SettingsInputSvideo
                "dvi" in lowerFunctionName -> Icons.Filled.SettingsInputHdmi
                "pc" in lowerFunctionName -> Icons.Filled.SettingsInputSvideo
                "channel" in lowerFunctionName -> Icons.Filled.SettingsInputAntenna
                "source" in lowerFunctionName -> Icons.AutoMirrored.Filled.Input
                "input" in lowerFunctionName -> Icons.AutoMirrored.Filled.Input
                "scan" in lowerFunctionName -> Icons.Filled.Radio
                "am" in lowerFunctionName -> Icons.Filled.Radio
                "fm" in lowerFunctionName -> Icons.Filled.Radio
                "radio" in lowerFunctionName -> Icons.Filled.Radio
                "repeat" in lowerFunctionName -> Icons.Filled.Repeat
                "encore" in lowerFunctionName -> Icons.Filled.Repeat
                "rating" in lowerFunctionName -> Icons.Filled.Star
                "shuffle" in lowerFunctionName -> Icons.Filled.Shuffle
                "playlist" in lowerFunctionName -> Icons.AutoMirrored.Filled.PlaylistPlay
                "freeze" in lowerFunctionName -> Icons.Filled.PauseCircle
                "a/v mute" in lowerFunctionName -> Icons.Filled.PauseCircle
                "mute" in lowerFunctionName -> Icons.AutoMirrored.Filled.VolumeMute
                "volume -" in lowerFunctionName -> Icons.AutoMirrored.Filled.VolumeDown
                "volume +" in lowerFunctionName -> Icons.AutoMirrored.Filled.VolumeUp
                "volume down" in lowerFunctionName || "volume dn" in lowerFunctionName -> Icons.AutoMirrored.Filled.VolumeDown
                "volume up" in lowerFunctionName -> Icons.AutoMirrored.Filled.VolumeUp
                "volume" in lowerFunctionName -> Icons.AutoMirrored.Filled.VolumeUp
                "help" in lowerFunctionName -> Icons.AutoMirrored.Filled.Help
                "home" in lowerFunctionName -> Icons.Filled.Home
                "color" in lowerFunctionName -> Icons.Filled.ColorLens
                "contrast" in lowerFunctionName -> Icons.Filled.BrightnessHigh
                "brightness" in lowerFunctionName -> Icons.Filled.BrightnessHigh
                "sharpness" in lowerFunctionName -> Icons.Filled.BrightnessHigh
                "tint" in lowerFunctionName -> Icons.Filled.BrightnessHigh
                "aspect" in lowerFunctionName -> Icons.Filled.AspectRatio
                "zoom" in lowerFunctionName -> Icons.Filled.ZoomIn
                else -> Icons.Filled.SettingsRemote
            }
        }

    @Composable
    fun asListItem(modifier: Modifier = Modifier) {
        ListItem(
            modifier = modifier,
            leadingContent = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),  // 40 is magic number for icon size in Material3 ListItem
                )
            },
            headlineContent = { Text(functionName) },
            supportingContent = { Text("${protocol.uppercase()} $device ($subdevice) $function") },
        )
    }
}
