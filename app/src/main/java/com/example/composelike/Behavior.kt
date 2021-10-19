package com.example.composelike

import android.os.Build
import androidx.annotation.RequiresApi

fun takePathToPlayer(actor: Actor, simulation: ComposelikeSimulation) {
    if (simulation.tilemap == null) error("This should never happen.")
    simulation.actors.getPlayer()?.let { player ->
        AStarPath.DirectActor(
            start = actor.coordinates,
            goal = player.coordinates,
            bounds = simulation.tilemap!!.mapRect.asBounds(),
            actor = actor,
            simulation = simulation
        ).path?.let { pathToPlayer ->
            if (pathToPlayer.size < 2) Unit
            else {
                val direction = actor.coordinates.relativeTo(pathToPlayer[1])
                simulation.actors.moveActor(actor, direction)
            }
        }
    }
}

sealed class Behavior(val effect: (actor: Actor, simulation: ComposelikeSimulation) -> Unit) {
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
     * The SimpleEnemy attacks the player if the player is adjacent, and moves randomly otherwise.
     */
    class SimpleEnemy : Behavior(
        effect = { actor, simulation ->
            simulation.actors.let { actors ->
                actors.getPlayer()?.let { player ->
                    if (player.isNeighbor(actor))
                        takePathToPlayer(actor, simulation)
                    else
                        actors.moveActor(actor, randomMovementDirection())
                }
            }
        }
    )

    /**
     * The HuntingEnemy runs an A* path to the player every turn and moves towards them as long as
     * there is a viable path, in order to attack.
     */
    @RequiresApi(Build.VERSION_CODES.N)
    class HuntingEnemy : Behavior(
        effect = { actor, simulation -> takePathToPlayer(actor, simulation) }
    )

    /**
     * The AmbushEnemy runs an A* path to the player in order to attack, if it can see them;
     * otherwise it remains stationary.
     */
    /*
        TODO: Currently the AmbushEnemy loses track of the player as soon as the player slips out of
            sight. There are a few different ways to fix that:
                1. Have the player leave a scent trail.
                2. Have each AmbushEnemy store a "last-seen" variable and move towards it when
                    it loses sight.
                ^ A combination of the above two is probably the way to go.
    */
    class AmbushEnemy : Behavior(
        effect = { actor, simulation ->
            simulation.actors.getPlayer()?.let { player ->
                simulation.tilemap?.getTileOrNull(player.coordinates)?.let { playerTile ->
                    if (actor.canSeeTile(playerTile))
                        takePathToPlayer(actor, simulation)
                    else
                        simulation.actors.moveActor(actor, MovementDirection.Stationary())
                }
            }
        }
    )
}