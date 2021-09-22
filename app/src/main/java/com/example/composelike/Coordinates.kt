package com.example.composelike

import kotlin.math.pow

data class Coordinates(val x: Int, val y: Int) {
    fun isNeighbor(other: Coordinates): Boolean {
        return kotlin.math.abs(x - other.x) <= 1 &&
               kotlin.math.abs(y - other.y) <= 1 &&
               !(other.x == x && other.y == y)
    }

    fun euclideanDistance(other: Coordinates): Int {
        return kotlin.math.sqrt(
            ((other.x - x).toDouble().pow(2) + (other.y - y).toDouble().pow(2))
        ).toInt()
    }
}