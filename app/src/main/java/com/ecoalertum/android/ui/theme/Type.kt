package com.ecoalertum.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ecoalertum.android.R

/**
 * Plus Jakarta Sans, the design-system font (SIL Open Font License 1.1,
 * https://github.com/tokotype/PlusJakartaSans). Files live in res/font.
 */
val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold),
)

private val base = Typography()

private fun TextStyle.jakarta() = copy(fontFamily = PlusJakartaSans)

// Type scale from the design system: Heading 26/800, Title 20/700, Body 16/500 (1.5), Caption 13/600.
// Every Material text style uses Plus Jakarta Sans.
val Typography = Typography(
    displayLarge = base.displayLarge.jakarta(),
    displayMedium = base.displayMedium.jakarta(),
    displaySmall = base.displaySmall.jakarta(),
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.ExtraBold).jakarta(),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.ExtraBold).jakarta(),
    headlineSmall = base.headlineSmall.copy(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.ExtraBold).jakarta(),
    titleLarge = base.titleLarge.copy(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold).jakarta(),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Bold).jakarta(),
    titleSmall = base.titleSmall.copy(fontWeight = FontWeight.Bold).jakarta(),
    bodyLarge = base.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium).jakarta(),
    bodyMedium = base.bodyMedium.copy(lineHeight = 21.sp, fontWeight = FontWeight.Medium).jakarta(),
    bodySmall = base.bodySmall.copy(fontWeight = FontWeight.Medium).jakarta(),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold).jakarta(),
    labelMedium = base.labelMedium.copy(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold).jakarta(),
    labelSmall = base.labelSmall.copy(fontWeight = FontWeight.SemiBold).jakarta(),
)
