package com.example.composelike

sealed class Behavior(
    val effect: (actor: Actor, simulation: ComposelikeSimulation) -> Unit
) {

    // TODO: Harmless wanderer behavior.

    class WanderingEnemy : Behavior(
        effect = { actor, simulation ->
            simulation.moveActor(actor, randomMovementDirection())
        }
    )

    class SimpleEnemy : Behavior(
        effect = { actor, simulation ->
            if (simulation.getPlayer().isNeighbor(actor)) {
                simulation.actorsFight(actor, simulation.getPlayer())
            } else {
                simulation.moveActor(actor, randomMovementDirection())
            }
        }
    )

    // TODO: Hunting enemy behavior.
}