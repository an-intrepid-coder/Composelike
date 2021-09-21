package com.example.composelike

sealed class Behavior(
    val effect: (actor: Actor, simulation: ComposelikeSimulation) -> Unit
) {

    // TODO: Harmless wanderer behavior.

    class WanderingEnemy : Behavior(
        effect = { actor, simulation ->
            simulation.actors.moveActor(actor, randomMovementDirection(), simulation)
        }
    )

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

    // TODO: Hunting enemy behavior.
}