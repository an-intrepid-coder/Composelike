package com.example.composelike

import kotlin.system.exitProcess

class ComposelikeSimulation(
    // TODO: Some options and parameters.
) {
    private var _turnsPassed = 0
    private var _dungeonlevel = 1
    private var _tilemap: Tilemap? = null

    // TODO: GameMessageLog class with Message/EventRecord class.
    private var _messageLog: List<String> = listOf()
    fun messageLog(): List<String> { return _messageLog }

    fun addLogMessage(msg: String) { _messageLog = _messageLog.plus(msg) }

    // TODO: Camera class
    private var _cameraCoordinates: Coordinates = Coordinates(0, 0)
    private var _cameraCoupled = true

    private fun snapCameraToPlayer() { _cameraCoordinates = getPlayer().coordinates }

    // TODO: ActorContainer class
    private var _actors: List<Actor> = listOf()

    fun addActor(actor: Actor) { _actors = _actors.plus(actor) }

    fun removeActor(actor: Actor) { _actors = _actors.minus(actor) }

    private fun actorCoordinates(): List<Coordinates> { return _actors.map { it.coordinates } }

    private fun getActorByCoordinates(coordinates: Coordinates): Actor {
        return _actors.first { it.coordinates == coordinates }
    }

    fun getPlayer(): Actor { return _actors.first { it.actorFaction == ActorFaction.PLAYER } }

    private fun tileIsOccupied(tile: Tile): Boolean { return actorCoordinates().contains(tile.coordinates) }

    fun actorsFight(attacker: Actor, defender: Actor) {
        if (attacker == defender) { return }
        // This is a placeholder combat system, for now:
        val totalDamage = 1 + attacker.bonusAttack - defender.bonusDefense
        _actors = _actors.minus(defender)
        defender.harm(totalDamage)
        addLogMessage("${attacker.name} did $totalDamage dmg to ${defender.name}.")
        if (defender.isAlive()) {
            _actors = _actors.plus(defender)
            addLogMessage("... it has ${defender.health} HP remaining.")
        } else {
            addLogMessage("... and killed it!")
            if (defender == getPlayer()) {
                exitProcess(0) // TODO: More graceful game-over condition!
            }
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
    fun moveActor(actor: Actor, movementDirection: MovementDirection) {
        val targetCoordinates = Coordinates(
            actor.coordinates.x + movementDirection.dx,
            actor.coordinates.y + movementDirection.dy
        )
        val targetTile = _tilemap?.getTileOrNull(targetCoordinates)
        if (targetTile != null) {
            if (targetTile.walkable && !tileIsOccupied(targetTile)) {
                var newActorsList = _actors.minus(actor)
                actor.coordinates = targetCoordinates
                newActorsList = newActorsList.plus(actor)
                _actors = newActorsList
            } else if (tileIsOccupied(targetTile)) {
                val defender = getActorByCoordinates(targetCoordinates)
                if (defender.actorFaction != actor.actorFaction) {
                    actorsFight(
                        attacker = actor,
                        defender = defender
                    )
                }
            } else if (actor == getPlayer()){
                addLogMessage("You can't move there!")
            }
        }
    }

    private fun updateActorBehavior() {
        for (actor in _actors) {
            if (actor.behavior != null) {
                actor.behavior!!.effect(actor, this)
            }
        }
    }

    // TODO: Concurrency!
    fun nextTurnByPlayerMove(movementDirection: MovementDirection): ComposelikeSimulation {
        // For now, Player will always go first. For now.
        moveActor(getPlayer(), movementDirection)
        updateActorBehavior()
        _turnsPassed++
        if (_cameraCoupled) { snapCameraToPlayer() }
        return this
    }

    // TODO: Concurrency
    fun takeEffect(effect: (ComposelikeSimulation) -> Unit) { effect(this) }

    // TODO: A HudStrings class.
    fun exportHudStrings(): Map<String, String> {
        val player = getPlayer()
        return mapOf(
            "hp" to "HP: " + player.health.toString() + "/" + player.maxHealth.toString(),
            "mp" to "MP: " + player.mana.toString() + "/" + player.maxMana.toString(),
            "bonusAttack" to "ATK: " + player.bonusAttack.toString(),
            "bonusDefense" to "DEF: " + player.bonusDefense.toString(),
            "gold" to "Gold: " + player.gold.toString(),
            "playerLevel" to "PLVL: " + player.level.toString(),
            // TODO: XP Bar!
            "experienceToLevel" to "XP-TO-GO: " + player.experienceToLevel.toString(),
            "dungeonLevel" to "DLVL: ${_dungeonlevel}",
            "turnsPassed" to "Turns: ${_turnsPassed}",
        )
    }

    // TODO: Display Strings class
    // TODO: Long-Term: CharacterCell class, replacing the String-based approach.
    private fun exportDisplayStrings(origin: Coordinates, ends: Coordinates): List<String>? {
        if (_tilemap == null) return null
        var newDisplayStrings = listOf<String>()
        for (row in origin.y until ends.y) {
            var rowString = ""
            for (col in origin.x until ends.x) {
                val tile = _tilemap!!.getTileOrNull(Coordinates(col, row))
                rowString += if (tile != null) {
                    if (Coordinates(col, row) in actorCoordinates()) {
                        getActorByCoordinates(Coordinates(col, row)).mapRepresentation
                    } else {
                        tile.mapRepresentation
                    }
                } else {
                    " "
                }
            }
            newDisplayStrings = newDisplayStrings.plus(rowString)
        }
        return newDisplayStrings
    }

    fun exportTilemapStrings(tilemapDisplayCols: Int, tilemapDisplayRows: Int): List<String>? {
        if (_tilemap == null) return null
        val origin = Coordinates(
            _cameraCoordinates.x - tilemapDisplayCols / 2,
            _cameraCoordinates.y - tilemapDisplayRows / 2
        )
        val ends = Coordinates(
            origin.x + tilemapDisplayCols,
            origin.y + tilemapDisplayRows
        )
        return exportDisplayStrings(origin, ends)
    }

    fun exportMapScreenStrings(): List<String>? {
        if (_tilemap == null) return null
        val origin = Coordinates(0, 0)
        val ends = Coordinates(_tilemap!!.cols, _tilemap!!.rows)
        return exportDisplayStrings(origin, ends)
    }

    /**
     * Returns a list of Coordinates at which an Actor could spawn (unoccupied walkable tiles).
     */
    private fun validSpawnCoordinates(): List<Coordinates>? {
        return _tilemap?.tiles()?.filter {
            !actorCoordinates().contains(it.coordinates) && it.walkable
        }?.map { it.coordinates }
    }

    /**
     * For now, generates a goblin for every 30 walkable tiles. Tentative.
     */
    private fun generateSmallGoblinPopulation() {
        val numGoblins = (_tilemap?.tiles()?.filter { it.walkable }?.size ?: 1000) / 30
        repeat (numGoblins) {
            validSpawnCoordinates()?.let {
                if (it.isNotEmpty()) _actors = _actors.plus(Actor.Goblin(it.random()))
            }
        }
    }

    private fun spawnPlayer() {
        validSpawnCoordinates()?.let {
            if (it.isNotEmpty()) _actors = _actors.plus(Actor.Player(it.random()))
        }
    }

    // TODO: This should be a suspend function.
    fun initSimulation() {
        _tilemap = Tilemap.Testing(100, 100)
        generateSmallGoblinPopulation()
        spawnPlayer()
        snapCameraToPlayer()
        addLogMessage("Welcome to Composelike!")
    }
}