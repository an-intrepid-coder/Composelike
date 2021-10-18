package com.example.composelike

import kotlin.math.abs
import kotlin.math.max

data class Bounds(val xBound: Int, val yBound: Int) {
    fun inBounds(coordinates: Coordinates): Boolean {
        return coordinates.x in 0..xBound && coordinates.y in 0..yBound
    }
}

data class Coordinates(val x: Int, val y: Int) {

    fun relativeTo(other: Coordinates): MovementDirection {
        val dx = other.x - this.x
        val dy = other.y - this.y
        return MovementDirection.Raw(dx, dy)
    }

    fun isNeighbor(other: Coordinates): Boolean {
        return abs(x - other.x) <= 1 &&
               abs(y - other.y) <= 1 &&
               !(other.x == x && other.y == y)
    }

    fun chebyshevDistance(other: Coordinates): Int {
        return max(abs(other.x - x), abs(other.y - y))
    }

    /**
     * Returns a Bresenham line between this coordinate and another as List<Coordinates>.
     */
    fun bresenhamLine(other: Coordinates): List<Coordinates> {
        /*
            Algorithm pseudocode courtesy of Wikipedia:
            https://en.wikipedia.org/wiki/Bresenham%27s_line_algorithm#All_cases
         */
        val line = mutableListOf<Coordinates>()
        var plottingX = x
        var plottingY = y
        val dx = abs(other.x - x)
        val dy = -abs(other.y - y)
        val sx = if (x < other.x) 1 else -1
        val sy = if (y < other.y) 1 else -1
        var err = dx + dy
        while (true) {
            if (plottingX == other.x && plottingY == other.y) break
            line.add(Coordinates(plottingX, plottingY))
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

    /**
     * Returns all neighbor coordinates that exist within the given bounds.
     */
    fun neighbors(bounds: Bounds): List<Coordinates> {
        return allMovementDirections
            .asSequence()
            .filter { it.name != "Stationary" }
            .map { Coordinates(this.x + it.dx, this.y + it.dy) }
            .filter { bounds.inBounds(it) }
            .toList()
    }
}