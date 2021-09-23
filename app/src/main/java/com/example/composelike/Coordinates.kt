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
        ).toInt() // TODO: Test and ensure that roundToInt() wouldn't be better here.
    }

    /**
     * Returns a Bresenham line between this coordinate and another as List<Coordinates>.
     */
    fun bresenhamLine(other: Coordinates): List<Coordinates> {
        /*
            Algorithm courtesy of Wikipedia:
            https://en.wikipedia.org/wiki/Bresenham%27s_line_algorithm#All_cases
         */
        var line = listOf<Coordinates>()
        var plottingX = x
        var plottingY = y
        val dx = kotlin.math.abs(other.x - x)
        val dy = -kotlin.math.abs(other.y - y)
        val sx = if (x < other.x) 1 else -1
        val sy = if (y < other.y) 1 else -1
        var err = dx + dy
        while (true) {
            if (plottingX == other.x && plottingY == other.y) break
            line = line.plus(Coordinates(plottingX, plottingY))
            val err2 = err * 2
            if (err2 >= dy) {
                err += dy
                plottingX += sx
            }
            if (err2 <= dx) {
                err += dx
                plottingY += sy
            }
        }
        return line
    }
}