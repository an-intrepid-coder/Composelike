package com.example.composelike

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin

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

    /**
     * Creates a single bend in the path.
     */
    fun randomElbowTo(other: Coordinates): List<Coordinates> {
        return if (coinFlip()) listOf(this, Coordinates(other.x, y), other)
        else listOf(this, Coordinates(x, other.y), other)
    }

    /**
     * This one is experimental and in progress. It attempts to create a random "wobble"
     * or "wave" from one point to another. This results in very unpredictable but neat looking
     * connections over long distances.
     */
    fun randomWobbleTo(
        other: Coordinates,
        amplitudeModifier: Double = 5.0,
        periodModifier: Double = .8,
        plotFrequency: Int = 5,
    ): List<Coordinates> {
        val points = mutableListOf<Coordinates>()

        val xDistance = abs(x - other.x) - 1
        val yDistance = abs(y - other.y) - 1
        val timeAndPlot =
            if (xDistance >= yDistance) Pair(xDistance, yDistance)
            else Pair(yDistance, xDistance)

        fun timeAxisAlreadyOriented(): Boolean {
            return if (timeAndPlot.first == xDistance) x <= other.x else y <= other.y
        }

        val startGoal = if (timeAxisAlreadyOriented()) Pair(this, other) else Pair(other, this)
        var swapped = false

        val amplitude =
            max(abs(x - other.x).toDouble(), abs(y - other.y).toDouble()) / amplitudeModifier

        fun plotPoint(plotIndex: Int): Coordinates {
            return if (timeAndPlot.first == xDistance) {
                if (!swapped) {
                    Coordinates(
                        x = startGoal.first.x + plotIndex,
                        y = startGoal.first.y +
                                (amplitude * sin(plotIndex.toDouble()) * periodModifier).toInt()
                    )
                } else {
                    Coordinates(
                        x = startGoal.first.x +
                                (amplitude * sin(plotIndex.toDouble()) * periodModifier).toInt(),
                        y = startGoal.first.y + plotIndex
                    )
                }
            } else {
                if (!swapped) {
                    Coordinates(
                        x = startGoal.first.x +
                                (amplitude * sin(plotIndex.toDouble()) * periodModifier).toInt(),
                        y = startGoal.first.y + plotIndex
                    )
                } else {
                    Coordinates(
                        x = startGoal.first.x + plotIndex,
                        y = startGoal.first.y +
                                (amplitude * sin(plotIndex.toDouble()) * periodModifier).toInt()
                    )
                }
            }
        }

        points.add(startGoal.first)

        fun checkSwap(coordinates: Coordinates): Boolean {
            val currentXDistance = abs(coordinates.x - startGoal.second.x) - 1
            val currentYDistance = abs(coordinates.y - startGoal.second.y) - 1
            if (!swapped && timeAndPlot.first == xDistance && currentXDistance < currentYDistance)
                return true
            if (!swapped && timeAndPlot.first == yDistance && currentYDistance < currentXDistance)
                return true
            return false
        }

        repeat(timeAndPlot.first) { plotIndex ->
            if (plotIndex % plotFrequency == 0) {
                val point = plotPoint(plotIndex)
                points.add(point)
                if (!swapped) swapped = checkSwap(point)
            }
        }

        points.add(startGoal.second)
        return points
    }
}