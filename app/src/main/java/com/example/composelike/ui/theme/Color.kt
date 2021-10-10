package com.example.composelike.ui.theme

import androidx.compose.ui.graphics.Color

val Purple200 = Color(0xFFBB86FC)
val Purple500 = Color(0xFF6200EE)
val Purple700 = Color(0xFF3700B3)
val Teal200 = Color(0xFF03DAC5)

// TODO: Long-Term: Integrate Colors with a Character Cell system -- eventually replacing the
//  current String-based approach. Will probably need to use the Canvas API which would then
//  lay the groundwork for Sprites instead of Character Cells (or the ability to toggle between
//  both, ideally).
val ElectricTeal = Color(10, 221, 245)
val DoublePlusGreen = Color(0, 255, 26)
val AlertRed = Color(255, 0, 0)
val CautionYellow = Color(245, 220, 0)
val ObsidianBlack = Color(0, 0, 0)
val StarkWhite = Color(255, 255, 255)
val VibrantMagenta = Color(240, 128, 255)
val DeepBlue = Color(0, 0, 255)

val cursesColors = listOf(
    ElectricTeal,
    DoublePlusGreen,
    AlertRed,
    CautionYellow,
    ObsidianBlack,
    StarkWhite,
    VibrantMagenta,
    DeepBlue
)