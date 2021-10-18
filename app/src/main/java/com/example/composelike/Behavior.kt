package com.example.composelike

import android.os.Build
import androidx.annotation.RequiresApi

fun takePathToPlayer(actor: Actor, simulation: ComposelikeSimulation) {
    val playerCoordinates = simulation.actors.getPlayer().coordinates

    val pathToPlayer = AStarPath.DirectActor(
        start = actor.coordinates,
        goal = playerCoordinates,
        bounds = Bounds(simulation.tilemap!!.numCols, simulation.tilemap!!.numRows),
        actor = actor,
        simulation = simulation
    ).path

    if (pathToPlayer.isNullOrEmpty()) Unit
    else if (pathToPlayer.size < 2) Unit
    else {
        val direction = actor.coordinates.relativeTo(pathToPlayer[1])
        simulation.actors.moveActor(actor, direction)
    }
}

sealed class Behavior(
    val effect: (actor: Actor, simulation: ComposelikeSimulation) -> Unit
) {
    // TODO: Harmless wanderer behavior.

    /**
     * The WanderingEnemy moves randomly and will attack the player if the player is unlucky.
     */
    class WanderingEnemy : Behavior(
        effect = { actor, simulation ->
            simulation.actors.moveActor(actor, randomMovementDirection())
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
                )
            } else {
                simulation.actors.moveActor(actor, randomMovementDirection())
            }
        }
    )

    /**
     * The HuntingEnemy runs an A* path to the player every turn and moves towards them as long as
     * there is a viable path, in order to attack.
     */
    @RequiresApi(Build.VERSION_CODES.N)
    class HuntingEnemy : Behavior(
        effect = { actor, simulation -> takePathToPlayer(actor, simulation)}
    )

    /**
     * The AmbushEnemy runs an A* path to the player in order to attack, if it can see them;
     * otherwise it remains stationary.
     */
    class AmbushEnemy : Behavior(
        effect = { actor, simulation ->
            val playerCoordinates = simulation.actors.getPlayer().coordinates
            simulation.tilemap?.getTileOrNull(playerCoordinates)?.let { playerTile ->
                if (actor.canSeeTile(playerTile))
                    takePathToPlayer(actor, simulation)
                else
                    simulation.actors.moveActor(actor, MovementDirection.Stationary())
            }
        }
    )
}