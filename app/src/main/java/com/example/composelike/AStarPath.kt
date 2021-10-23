package com.example.composelike

import android.os.Build
import androidx.annotation.RequiresApi
import java.util.*

const val scoreDefault = Int.MAX_VALUE / 2

/**
 * Finds the shortest path from one set of Coordinates to another using the A* search algorithm.
 * Can be injected with waypoints and custom heuristics.
 */
@RequiresApi(Build.VERSION_CODES.N)
/*
    Requires API Level 24. This propagates up in a way that suggests I should eventually
    raise the minimum API level of the whole App, or else avoid using the PriorityQueue
    constructor which required it. I'm leaning towards the former, as I think that
    constructor is very good style.

    TODO: Look into raising the API level of the entire App.
 */
sealed class AStarPath(
    waypoints: List<Coordinates>,
    bounds: Bounds,
    heuristicFunction: (Coordinates, Coordinates, Actor?, ComposelikeSimulation?) -> Int =
        { node, goal, _, _ -> node.chebyshevDistance(goal) },
    actor: Actor? = null,
    simulation: ComposelikeSimulation? = null,
) {
    /*
        Algorithm pseudocode courtesy of Wikipedia:
        https://en.wikipedia.org/wiki/A*_search_algorithm

        Tips on A* performance courtesy of Reddit:
        https://www.reddit.com/r/roguelikedev/comments/59u44j/warning_a_and_manhattan_distance/
     */
    var path: List<Coordinates>? = null

    init {
        var finalPath = listOf<Coordinates>()

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
            // TODO: ^ I may still be able to optimize this part.
        }

        if (waypoints.size < 2) error("Waypoint size < 2")

        val mutableWaypoints = waypoints.toMutableList()
        var currentWaypoint = mutableWaypoints.removeFirst()
        var nextWaypoint: Coordinates? = mutableWaypoints.removeFirst()

        fun connectWaypoints(): Boolean {
            val cameFrom = mutableMapOf<Coordinates, Coordinates>()

            val gScore = mutableMapOf<Coordinates, Int>()
            gScore[currentWaypoint] = 0

            val fScore = mutableMapOf<Coordinates, Int>()

            val openSet = PriorityQueue { a: Coordinates, b: Coordinates ->
                val fScoreA = fScore.getOrElse(a) { scoreDefault }
                val fScoreB = fScore.getOrElse(b) { scoreDefault }

                if (fScoreA < fScoreB) -1
                else if (fScoreA > fScoreB) 1
                else 0
            }
            openSet.add(currentWaypoint)

            while (!openSet.isEmpty()) {
                val currentNode = openSet.remove()

                if (currentNode == nextWaypoint) {
                    finalPath = finalPath.plus(reconstructPath(cameFrom, currentNode))
                    return true
                }

                val gScoreCurrent = gScore.getOrElse(currentNode) { scoreDefault }

                currentNode.neighbors(bounds).forEach { node ->
                    val gScoreNode = gScore.getOrElse(node) { scoreDefault }
                    val tentativeGScore = gScoreCurrent + currentNode.chebyshevDistance(node)

                    if (tentativeGScore < gScoreNode) {
                        cameFrom[node] = currentNode
                        gScore[node] = tentativeGScore
                        val hScore = heuristicFunction(node, nextWaypoint!!, actor, simulation)
                        fScore[node] = gScore[node]!! + hScore
                        if (node !in openSet && hScore < scoreDefault) openSet.add(node)
                    }
                }
            }
            return false
        }

        var pathFound = false
        do {
            if (connectWaypoints()) {
                nextWaypoint?.let { currentWaypoint = it }
                nextWaypoint = mutableWaypoints.removeFirstOrNull()
                pathFound = true
            } else break
        } while (mutableWaypoints.isNotEmpty())
        if (pathFound) path = finalPath
    }

    class Direct(
        start: Coordinates,
        goal: Coordinates,
        bounds: Bounds,
    ) : AStarPath(
        waypoints = listOf(start, goal),
        bounds = bounds,
    )

    class DirectSequence(
        waypoints: List<Coordinates>,
        bounds: Bounds,
    ) : AStarPath(
        waypoints = waypoints,
        bounds = bounds,
    )

    class DirectActor(
        /*
            TODO: Optimization: By having each actor store a saved path and only recomputing it
             when needed or every-so-often, I can vastly speed things up.
         */
        start: Coordinates,
        goal: Coordinates,
        bounds: Bounds,
        actor: Actor,
        simulation: ComposelikeSimulation,
    ) : AStarPath(
        waypoints = listOf(start, goal),
        bounds = bounds,
        heuristicFunction = { node, goal, actor, simulation ->
            val targetTile = simulation?.tilemap?.getTileOrNull(node)
            val walkable = targetTile?.walkable
            val occupiedByFriendly = targetTile?.let {
                simulation.actors.getActorByCoordinates(targetTile.coordinates)?.let { occupant ->
                    occupant.actorFaction == actor?.actorFaction
                }
            }
            if (walkable == true && occupiedByFriendly != true)
                node.chebyshevDistance(goal)
            else scoreDefault
        },
        actor = actor,
        simulation = simulation
    )
}