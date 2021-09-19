package com.example.composelike

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlin.system.exitProcess

// TODO: Field of View

// TODO: Character cells instead of full String rows; will enable better use of color.

class GameViewModel(
    val tilemap: Tilemap,
    // TODO: Camera object
    var cameraCoordinates: Coordinates = Coordinates(0, 0),
    var cameraCoupled: Boolean = true,
) : ViewModel() {
    // These dimensions are tentative and will be refined:
    private var _tilemapDisplayCols = 36
    private var _tilemapDisplayRows = 12

    private var _turnsPassed = MutableLiveData(0)
    val turnsPassed: LiveData<Int> = _turnsPassed

    fun incrementTurnsPassed() { _turnsPassed.value = _turnsPassed.value!! + 1 }

    private var _dungeonLevel = MutableLiveData(1)
    val dungeonlevel: LiveData<Int> = _dungeonLevel

    fun incrementDungeonlevel() { _dungeonLevel.value = _dungeonLevel.value!! + 1 }

    fun decrementDungeonlevel() { _dungeonLevel.value = _dungeonLevel.value!! - 1 }

    fun snapCameraToPlayer() { cameraCoordinates = getPlayer().coordinates }

    // TODO: An ActorsList or ActorsContainer class.
    private var _actors = MutableLiveData<List<Actor>>(listOf())
    var actors: LiveData<List<Actor>> = _actors

    fun addActor(actor: Actor) {
        _actors.value = _actors.value!!.plus(actor)
    }

    fun removeActor(actor: Actor) {
        _actors.value = _actors.value!!.minus(actor)
    }

    fun actorCoordinates(): List<Coordinates> {
        return actors.value!!.map { it.coordinates }
    }

    fun getActorByCoordinates(coordinates: Coordinates): Actor {
        return actors.value!!.first { it.coordinates == coordinates }
    }

    fun getPlayer(): Actor {
        return _actors.value!!.first { it.actorFaction == ActorFaction.PLAYER }
    }

    fun tileIsOccupied(tile: Tile): Boolean {
        return actorCoordinates().contains(tile.coordinates)
    }

    fun actorsFight(attacker: Actor, defender: Actor) {
        if (attacker == defender) { return }
        // This is a placeholder combat system, for now:
        val totalDamage = 1 + attacker.bonusAttack - defender.bonusDefense
        _actors.value = _actors.value!!.minus(defender)
        defender.health -= totalDamage
        addLogMessage("${attacker.name} did $totalDamage dmg to ${defender.name}.")
        if (defender.isAlive()) {
            _actors.value = _actors.value!!.plus(defender)
            addLogMessage("... it has ${defender.health} HP remaining.")
        } else {
            addLogMessage("... and killed it!")
            if (defender == getPlayer()) {
                exitProcess(0) // TODO: More graceful game-over condition!
            }
            if (attacker == getPlayer()) {
                // Only the player will get XP this way, for now.
                _actors.value = _actors.value!!.minus(attacker)
                attacker.rewardXp(10 * defender.level) // tentative
                _actors.value = _actors.value!!.plus(attacker)
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
        val targetTile = tilemap.getTileOrNull(targetCoordinates)
        if (targetTile != null) {
            if (targetTile.walkable && !tileIsOccupied(targetTile)) {
                var newActorsList = actors.value!!.minus(actor)
                actor.coordinates = targetCoordinates
                newActorsList = newActorsList.plus(actor)
                _actors.value = newActorsList
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
        for (actor in _actors.value!!) {
            when (actor.behaviorType) {
                BehaviorType.WANDERING -> wanderingBehavior(actor)
                BehaviorType.SIMPLE_ENEMY -> simpleEnemyBehavior(actor)
                // Many more to come!
                else -> Unit
            }
        }
    }

    /**
     * This is the big "next turn" function. Calling it advances the simulation by one turn.
     */
    fun movePlayerAndProcessTurn(movementDirection: MovementDirection) {
        moveActor(getPlayer(), movementDirection)
        // For now, Player will always go first. For now.
        updateActorBehavior()
        incrementTurnsPassed()
        if (cameraCoupled) { snapCameraToPlayer() }
        updateHudStrings()
        updateTilemapStrings()
        updateInventoryEntries()
        // TODO: Although it would be a *slight* performance hit, updateMapScreenStrings()
        //  may need to happen here too instead of only on navigation. It depends on testing
        //  once gameplay is more advanced.
    }

    private var _hudStrings = MutableLiveData<Map<String, String>>(mapOf())
    var hudStrings: LiveData<Map<String, String>> = _hudStrings

    fun updateHudStrings() {
        val player = getPlayer()
        _hudStrings.value = mapOf(
            "hp" to "HP: " + player.health.toString() + "/" + player.maxHealth.toString(),
            "mp" to "MP: " + player.mana.toString() + "/" + player.maxMana.toString(),
            "bonusAttack" to "ATK: " + player.bonusAttack.toString(),
            "bonusDefense" to "DEF: " + player.bonusDefense.toString(),
            "gold" to "Gold: " + player.gold.toString(),
            "playerLevel" to "PLVL: " + player.level.toString(),
            // TODO: XP Bar!
            "experienceToLevel" to "XP-TO-GO: " + player.experienceToLevel.toString(),
            "dungeonLevel" to "DLVL: ${dungeonlevel.value!!}",
            "turnsPassed" to "Turns: ${turnsPassed.value!!}",
        )
    }

    private var _tilemapStrings = MutableLiveData<List<String>>(listOf())
    var tilemapStrings: LiveData<List<String>> = _tilemapStrings

    private fun displayStrings(origin: Coordinates, ends: Coordinates): List<String> {
        var newDisplayStrings = listOf<String>()
        for (row in origin.y until ends.y) {
            var rowString = ""
            for (col in origin.x until ends.x) {
                val tile = tilemap.getTileOrNull(Coordinates(col, row))
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

    fun updateTilemapStrings() {
        val origin = Coordinates(
            cameraCoordinates.x - _tilemapDisplayCols / 2,
            cameraCoordinates.y - _tilemapDisplayRows / 2
        )
        val ends = Coordinates(
            origin.x + _tilemapDisplayCols,
            origin.y + _tilemapDisplayRows
        )
        _tilemapStrings.value = displayStrings(origin, ends)
    }

    private var _mapScreenStrings = MutableLiveData<List<String>>(listOf())
    var mapScreenStrings: LiveData<List<String>> = _mapScreenStrings

    fun updateMapScreenStrings() {
        val origin = Coordinates(0, 0)
        val ends = Coordinates(tilemap.cols, tilemap.rows)
        _mapScreenStrings.value = displayStrings(origin, ends)
    }

    private var _messageLog = MutableLiveData<List<String>>(listOf())
    // TODO: LoggedAction or LoggedEvent data class.
    var messageLog = _messageLog

    fun addLogMessage(msg: String) {
        _messageLog.value = _messageLog.value!!.plus(msg)
    }

    private var _inventoryEntries = MutableLiveData<List<Item>>(listOf())
    var inventoryEntries: LiveData<List<Item>> = _inventoryEntries

    fun updateInventoryEntries() {
        var newEntries = listOf<Item>()
        val playerInventory = getPlayer().inventory
        for (item in playerInventory) {
            newEntries = newEntries.plus(item)
        }
        _inventoryEntries.value = newEntries
    }

    /**
     * Returns a list of Coordinates at which an Actor could spawn (unoccupied walkable tiles).
     */
    fun validSpawnCoordinates(): List<Coordinates> {
        return tilemap.tiles().filter {
            !actorCoordinates().contains(it.coordinates) && it.walkable
        }.map { it.coordinates }
    }

    /**
     * For now, generates a goblin for every 30 walkable tiles. Tentative.
     */
    fun generateSmallGoblinPopulation() {
        val numGoblins = tilemap.tiles().filter { it.walkable }.size / 30
        repeat (numGoblins) {
            val spawnCoordinates = validSpawnCoordinates()
            if (spawnCoordinates.isNotEmpty()) {
                _actors.value = _actors.value!!.plus(weakGoblin(
                    coordinates = spawnCoordinates.random()
                ))
            }
        }
    }

    // TODO: Harmless wanderer behavior.

    /**
     * Will wander in a random direction, if possible. Will not attack Actors of the same
     * faction.
     */
    fun wanderingBehavior(actor: Actor) { moveActor(actor, randomMovementDirection()) }

    /**
     * If an Actor of a hostile faction is adjacent, the Actor will attack. Otherwise, it will
     * wander randomly.
     */
    fun simpleEnemyBehavior(actor: Actor) {
        if (getPlayer().isNeighbor(actor)) {
            actorsFight(actor, getPlayer())
        } else {
            wanderingBehavior(actor)
        }
    }

    // TODO: Hunting enemy behavior.

    init {
        // TODO: Some loading screen triggers will go here, most likely.
        addLogMessage("Welcome to Composelike!")
        addLogMessage("You must find the Orb of Victory.")
        addLogMessage("It is somewhere deep below...")
        // TODO: A newPlayer() function:
        _actors.value = _actors.value!!.plus(newPlayer(
            coordinates = validSpawnCoordinates().random()
        ))
        generateSmallGoblinPopulation()
        snapCameraToPlayer()
        updateTilemapStrings()
        updateHudStrings()
    }
}

class GameViewModelFactory(val tilemap: Tilemap): ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED CAST")
        return GameViewModel(tilemap) as T
    }
}