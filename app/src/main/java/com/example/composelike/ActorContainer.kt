package com.example.composelike

import kotlin.system.exitProcess

class ActorContainer {
    private var _actors = listOf<Actor>()
    fun actors() = _actors

    fun addActor(actor: Actor) { _actors = _actors.plus(actor) }

    fun removeActor(actor: Actor) { _actors = _actors.minus(actor) }

    fun actorCoordinates(): List<Coordinates> { return _actors.map { it.coordinates } }

    fun getActorByCoordinates(coordinates: Coordinates): Actor? {
        return _actors.firstOrNull { it.coordinates == coordinates }
    }

    fun getPlayer(): Actor {
        val player = _actors.firstOrNull { it.actorFaction == ActorFaction.PLAYER }
        if (player == null) exitProcess(0) // TODO: Real game over function.
        else return player
    }

    fun actorsFight(
        attacker: Actor,
        defender: Actor,
        simulation: ComposelikeSimulation
    ) {
        if (attacker == defender) { return }
        // This is a placeholder combat system, for now:
        val totalDamage = 1 + attacker.bonusAttack - defender.bonusDefense
        _actors = _actors.minus(defender)
        defender.harm(totalDamage)
        simulation
            .messageLog.addMessage("${attacker.name} did $totalDamage dmg to ${defender.name}.")
        if (defender.isAlive()) {
            _actors = _actors.plus(defender)
            simulation.messageLog.addMessage("... it has ${defender.health} HP remaining.")
        } else {
            simulation.messageLog.addMessage("... and killed it!")
            if (attacker == getPlayer()) {
                // Only the player will get XP this way, for now.
                _actors = _actors.minus(attacker)
                attacker.rewardXp(10 * defender.level) // tentative
                _actors = _actors.plus(attacker)
            }
        }
    }

    /**
     * Attempts to move the given Actor in the given movement Direction.
     * Will fight an Actor of a different faction if one is at the intended destination.
     */
    fun moveActor(
        // TODO: This can be optimized.
        actor: Actor,
        movementDirection: MovementDirection,
        simulation: ComposelikeSimulation,
    ) {
        val targetCoordinates = Coordinates(
            actor.coordinates.x + movementDirection.dx,
            actor.coordinates.y + movementDirection.dy
        )
        val targetTile = simulation.tilemap?.getTileOrNull(targetCoordinates)
        if (targetTile != null) {
            val tileIsOccupied = simulation.actors.actorCoordinates()
                .contains(targetTile.coordinates)
            if (targetTile.walkable && !tileIsOccupied) {
                simulation.actors.removeActor(actor)
                actor.coordinates = targetCoordinates
                simulation.actors.addActor(actor)
            } else if (tileIsOccupied) {
                val defender = simulation.actors.getActorByCoordinates(targetCoordinates)
                if (defender!!.actorFaction != actor.actorFaction) {
                    simulation.actors.actorsFight(
                        attacker = actor,
                        defender = defender,
                        simulation = simulation
                    )
                }
            } else if (actor == simulation.actors.getPlayer()){
                simulation.messageLog.addMessage("You can't move there!")
            }
        }
    }

    fun updateActorBehavior(simulation: ComposelikeSimulation) {
        for (actor in _actors) {
            if (actor.behavior != null) {
                actor.behavior!!.effect(actor, simulation)
            }
        }
    }

}