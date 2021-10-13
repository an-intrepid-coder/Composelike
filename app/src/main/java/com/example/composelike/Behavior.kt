package com.example.composelike

import android.os.Build
import androidx.annotation.RequiresApi

sealed class Behavior(
    val effect: (actor: Actor, simulation: ComposelikeSimulation) -> Unit
) {

    // TODO: Harmless wanderer behavior.

    /**
     * The WanderingEnemy moves randomly and will attack the player if the player is unlucky.
     */
    class WanderingEnemy : Behavior(
        effect = { actor, simulation ->
            simulation.actors.moveActor(actor, randomMovementDirection(), simulation)
        }
    )

    /**
     * The SimpleEnemy attacks the player if the player is adjacent and moves randomly otherwise.
     */
    class SimpleEnemy : Behavior(
        effect = { actor, simulation ->
            if (simulation.actors.getPlayer().isNeighbor(actor)) {
                simulation.actors.actorsFight(
                    attacker = actor,
                    defender = simulation.actors.getPlayer(),
                    simulation = simulation,
                )
            } else {
                simulation.actors.moveActor(actor, randomMovementDirection(), simulation)
            }
        }
    )

    /**
     * The HuntingEnemy runs an A* path to the player every turn.
     */
    @RequiresApi(Build.VERSION_CODES.N)
    class HuntingEnemy : Behavior(
        effect = { actor, simulation ->
            val playerLocation = simulation.actors.getPlayer().coordinates

            val pathToPlayer = actor.coordinates.shortestPathTo(
                goal = playerLocation,
                xBound = simulation.tilemap!!.cols,
                yBound = simulation.tilemap!!.rows,
                actor = actor,
                simulation = simulation,
                heuristicFunction = { node, actor, simulation ->
                    val scoreDefault = Int.MAX_VALUE / 2
                    val targetTile = simulation?.tilemap?.getTileOrNull(node)
                    val walkable = targetTile?.walkable
                    val occupiedByFriendly = targetTile?.let {
                        simulation.actors.getActorByCoordinates(targetTile.coordinates)?.let {
                            it.actorFaction == actor?.actorFaction
                        }
                    }

                    if (walkable == true && occupiedByFriendly != true)
                        node.chebyshevDistance(playerLocation)
                    else scoreDefault
                }
            )

            if (pathToPlayer.isNullOrEmpty()) Unit
            else if (pathToPlayer.size < 2) Unit
            else {
                val direction = actor.coordinates.relativeTo(pathToPlayer[1])
                simulation.actors.moveActor(actor, direction, simulation)
            }
        }
    )

    // TODO: Ambush enemy which hunts in only certain areas and is stationary/hidden otherwise.
}