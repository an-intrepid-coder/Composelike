package com.example.composelike

import kotlin.system.exitProcess

class ActorContainer(
    val parentSimulation: ComposelikeSimulation
) {
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
        if (player == null) exitProcess(0)
        // TODO: On player defeat, save a high score to persistent storage and present the
        //  player with game statistics and the option for a new game.
        else return player
    }

    fun actorsFight(
        attacker: Actor,
        defender: Actor,
    ) {
        if (attacker == defender) { return }
        // This is a placeholder combat system, for now:
        val totalDamage = 1 + attacker.bonusAttack - defender.bonusDefense
        _actors = _actors.minus(defender)
        defender.harm(totalDamage)
        parentSimulation.messageLog.addMessage(
            "${attacker.name} did $totalDamage dmg to ${defender.name}."
        )
        if (defender.isAlive()) {
            _actors = _actors.plus(defender)
            parentSimulation.messageLog.addMessage("... it has ${defender.health} HP remaining.")
        } else {
            parentSimulation.messageLog.addMessage("... and killed it!")
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
        actor: Actor,
        movementDirection: MovementDirection,
    ) {
        val targetCoordinates = Coordinates(
            x = actor.coordinates.x + movementDirection.dx,
            y = actor.coordinates.y + movementDirection.dy
        )
        parentSimulation.tilemap?.getTileOrNull(targetCoordinates)?.let { targetTile ->
            val tileIsOccupied = parentSimulation.actors
                .actorCoordinates()
                .contains(targetTile.coordinates)

            if (targetTile.walkable && !tileIsOccupied) {
                parentSimulation.actors.removeActor(actor)
                actor.coordinates = targetCoordinates
                parentSimulation.actors.addActor(actor)
            } else if (!targetTile.walkable && actor == parentSimulation.actors.getPlayer()) {
                parentSimulation.messageLog.addMessage("Can't move there!")
            } else if (tileIsOccupied) {
                parentSimulation.actors.getActorByCoordinates(targetCoordinates)?.let { defender ->
                    if (defender.actorFaction != actor.actorFaction) {
                        parentSimulation.actors.actorsFight(
                            attacker = actor,
                            defender = defender
                        )
                    }
                }
            }
        }
    }

    fun updateActorBehavior(simulation: ComposelikeSimulation) {
        _actors.forEach { actor ->
            actor.behavior?.let { action ->
                action.effect(actor, simulation)
            }
        }
    }

}