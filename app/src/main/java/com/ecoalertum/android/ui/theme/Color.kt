package com.ecoalertum.android.ui.theme

import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------------------------
// EcoAlert UM design system colours (owned by UI/UX). Use MaterialTheme.colorScheme in screens;
// use these names directly only for things the colour scheme has no slot for.
// ---------------------------------------------------------------------------------------------

// Brand
val PrimaryTeal = Color(0xFF0F4C47)   // app bars, primary buttons
val TealTint = Color(0xFFE3F0EC)      // selected states, AI bubbles
val LeafGreen = Color(0xFF1F7A4A)     // correct answers, progress
val LeafTint = Color(0xFFE2F2E8)
val AlertAmber = Color(0xFFF2A23A)    // highlights, badges (always dark text on it)
val AmberTint = Color(0xFFFDEBD3)
val AmberInk = Color(0xFF7A4A0B)      // text on AmberTint

// Neutrals
val Ink = Color(0xFF10201E)
val Muted = Color(0xFF4A5B58)
val Line = Color(0xFFDDE5E2)
val Background = Color(0xFFF4F7F5)

// Hazard severity: always show the word (Advisory, Warning, Danger) next to the colour
val SeverityNormal = Color(0xFF1F7A4A)
val SeverityAdvisory = Color(0xFFF2A23A)
val SeverityWarning = Color(0xFFC2410C)
val SeverityDanger = Color(0xFF9F1D14)

// Dark theme versions
val PrimaryTealDark = Color(0xFF7FD1C4)
val TealTintDark = Color(0xFF1C3A36)
val InkDark = Color(0xFFE3ECE9)
val BackgroundDark = Color(0xFF0F1716)
