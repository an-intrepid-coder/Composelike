package com.example.composelike

import kotlin.math.abs
import kotlin.math.max

data class Coordinates(val x: Int, val y: Int) {

    fun bounded(bounds: Bounds): Coordinates {
        return Coordinates(
            x = if (x in bounds.xRange) x else {
                if (x < bounds.xRange.first) bounds.xRange.first
                else bounds.xRange.last
            },
            y = if (y in bounds.yRange) y else {
                if (y < bounds.yRange.first) bounds.yRange.first
                else bounds.yRange.last
            }
        )
    }

    fun relativeTo(other: Coordinates): MovementDirection {
        val dx = other.x - x
        val dy = other.y - y
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

    // TODO: More parameters for randomElbowTo() and randomWobbleTo().

    fun randomElbowTo(other: Coordinates): List<Coordinates> {
        return if (coinFlip()) listOf(this, Coordinates(other.x, y), other)
        else listOf(this, Coordinates(x, other.y), other)
    }

    fun randomWobbleTo(other: Coordinates): List<Coordinates> {
        val bounds = Bounds(
            xRange = 1..max(x, other.x),
            yRange = 1..max(y, other.y)
        )
        return listOf() // TODO: Next
    }
}