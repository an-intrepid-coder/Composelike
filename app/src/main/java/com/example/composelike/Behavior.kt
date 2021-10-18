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

            val pathToPlayer = AStarPath.DirectActor(
                start = actor.coordinates,
                goal = playerLocation,
                bounds = Bounds(simulation.tilemap!!.numCols, simulation.tilemap!!.numRows),
                actor = actor,
                simulation = simulation
            ).path

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