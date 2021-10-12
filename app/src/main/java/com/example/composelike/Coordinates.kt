package com.example.composelike

import android.os.Build
import androidx.annotation.RequiresApi
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow

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

    fun euclideanDistance(other: Coordinates): Int {
        return kotlin.math.sqrt(
            ((other.x - x).toDouble().pow(2) + (other.y - y).toDouble().pow(2))
        ).toInt()
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
     * Returns all neighbor coordinates that exist within the simulation (no out of bounds
     * coordinates).
     */
    private fun neighbors(xBound: Int, yBound: Int): List<Coordinates> {
        return allMovementDirections
            .asSequence()
            .filter { it.name != "Stationary" }
            .map { Coordinates(this.x + it.dx, this.y + it.dy) }
            .filter { it.x >= 0 && this.x < xBound && it.y >= 0 && it.y < yBound }
            .toList()
    }

    /**
     * Finds the shortest path from one set of Coordinates to another using the A* search algorithm.
     *
     * TODO: It's going to get a little funky using this for map generation and also for
     *  pathfinding. I'll need to make the parameters more complicated or else split this into
     *  multiple functions with different sets of parameters, eventually.
     *
     * TODO: Waypoints!
     */
    // Requires API Level 24.
    @RequiresApi(Build.VERSION_CODES.N)
    fun shortestPathTo(
        goal: Coordinates,
        xBound: Int,
        yBound: Int,
        actor: Actor? = null,
        simulation: ComposelikeSimulation? = null,
        heuristicFunction: (Coordinates, Actor?, ComposelikeSimulation?) -> Int =
            { node, actor, simulation -> node.chebyshevDistance(goal) }
    ): List<Coordinates>? {
        /*
            Algorithm pseudocode courtesy of Wikipedia:
            https://en.wikipedia.org/wiki/A*_search_algorithm

            Tips on A* performance courtesy of Reddit:
            https://www.reddit.com/r/roguelikedev/comments/59u44j/warning_a_and_manhattan_distance/

            TODO: Optimizations during map generation and pathfinding to help out this algorithm
                in the worst-case, such as when there is no viable path to the player or the map
                is not contiguous.
         */

        val scoreDefault = Int.MAX_VALUE / 2

        fun reconstructPath(
            cameFrom: Map<Coordinates, Coordinates>,
            current: Coordinates
        ): List<Coordinates> {
            val totalPath = mutableListOf(current)
            var temp = current
            while (temp in cameFrom.keys) {
                temp = cameFrom[temp]!!
                totalPath.add(temp)
            }
            return totalPath.reversed()
        }

        val cameFrom = mutableMapOf<Coordinates, Coordinates>()

        val gScore = mutableMapOf<Coordinates, Int>()
        gScore[this] = 0

        val fScore = mutableMapOf<Coordinates, Int>()

        val openSet = PriorityQueue { a: Coordinates, b: Coordinates ->
            val fScoreA = fScore.getOrElse(a) { scoreDefault }
            val fScoreB = fScore.getOrElse(b) { scoreDefault }

            if (fScoreA < fScoreB) -1
            else if (fScoreA > fScoreB) 1
            else 0
        }
        openSet.add(this)

        while (!openSet.isEmpty()) {
            val current = openSet.remove()

            if (current == goal) return reconstructPath(cameFrom, current)

            val gScoreCurrent = gScore.getOrElse(current) { scoreDefault }

            for (node in current.neighbors(xBound, yBound)) {
                val gScoreNode = gScore.getOrElse(node) { scoreDefault }
                val tentativeGScore = gScoreCurrent + current.chebyshevDistance(node)
                if (tentativeGScore < gScoreNode) {
                    cameFrom[node] = current
                    gScore[node] = tentativeGScore
                    fScore[node] = gScore[node]!! + heuristicFunction(node, actor, simulation)
                    if (node !in openSet) openSet.add(node)
                }
            }
        }
        return null
    }
}