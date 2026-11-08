package com.ecoalertum.android.home

import androidx.compose.ui.graphics.Color
import com.ecoalertum.android.core.data.model.Level

// ---------------------------------------------------------------------------------------------
// Severity colours from the design system, used only by the Home screen for now.
// TODO(UI/UX): move these into ui/theme once the theme defines severity colours.
// ---------------------------------------------------------------------------------------------

/** Background colour for a level. Always show it together with the level word. */
fun Level.color(): Color = when (this) {
    Level.NORMAL -> Color(0xFF1F7A4A)
    Level.ADVISORY -> Color(0xFFF2A23A)
    Level.WARNING -> Color(0xFFC2410C)
    Level.DANGER -> Color(0xFF9F1D14)
}

/** Text colour on [color]: dark on Advisory, white on the others. */
fun Level.onColor(): Color = when (this) {
    Level.ADVISORY -> Color(0xFF1C1B1F)
    else -> Color.White
}
