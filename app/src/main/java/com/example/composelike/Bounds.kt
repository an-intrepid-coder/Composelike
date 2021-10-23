package com.example.composelike

data class Bounds(
    val xRange: IntRange,
    val yRange: IntRange) {
    fun inBounds(coordinates: Coordinates): Boolean {
        return coordinates.x in xRange && coordinates.y in yRange
    }
}
