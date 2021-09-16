package com.example.composelike

data class Coordinates(val x: Int, val y: Int) {
    // TODO: Custom (==) function for (x, y) == (x, y)
    fun isNeighbor(other: Coordinates): Boolean {
        return kotlin.math.abs(x - other.x) <= 1 &&
               kotlin.math.abs(y - other.y) <= 1 &&
               other.x != x && other.y != y
    }
}