package com.example.composelike

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

enum class TileType {
    WALL,
    FLOOR
}

enum class TilemapType {
    TESTING,
}

data class Tile(val coordinates: Coordinates, val tileType: TileType)

enum class MovementDirection {
    UP,
    DOWN,
    LEFT,
    RIGHT,
    UPLEFT,
    UPRIGHT,
    DOWNLEFT,
    DOWNRIGHT,
    STATIONARY,
    // More to come, perhaps...
}

data class MovementDeltas(val dx: Int, val dy: Int)

val movementDeltas = mapOf(
    MovementDirection.UP to MovementDeltas(0, -1),
    MovementDirection.DOWN to MovementDeltas(0, 1),
    MovementDirection.LEFT to MovementDeltas(-1, 0),
    MovementDirection.RIGHT to MovementDeltas(1, 0),
    MovementDirection.UPLEFT to MovementDeltas(-1, -1),
    MovementDirection.UPRIGHT to MovementDeltas(1, -1),
    MovementDirection.DOWNLEFT to MovementDeltas(-1, 1),
    MovementDirection.DOWNRIGHT to MovementDeltas(1, 1),
    MovementDirection.STATIONARY to MovementDeltas(0, 0)
)

class SceneViewModel(
    val tilemapCols: Int,
    val tilemapRows: Int,
    val tilemapType: TilemapType,
    var cameraCoordinates: Coordinates = Coordinates(0, 0),
    var cameraCoupled: Boolean = true,
) : ViewModel() {
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

    private var _tiles = MutableLiveData<List<List<Tile>>>(listOf())
    var tiles: LiveData<List<List<Tile>>> = _tiles
    fun getTileOrNull(coordinates: Coordinates): Tile? {
        return tiles.value?.getOrNull(coordinates.y)?.getOrNull(coordinates.x)
    }
    fun walkableTileType(tile: Tile): Boolean {
        return when (tile.tileType) {
            TileType.WALL -> false
            else -> true
        }
        // This will grow down the road.
    }
    fun isEdgeCoordinate(coordinates: Coordinates): Boolean {
        val col = coordinates.x
        val row = coordinates.y
        return (row == 0 || col == 0 || row == tilemapRows - 1 || col == tilemapCols - 1)
    }
    private fun generateTestingMap(): List<List<Tile>> {
        var newTilemap: List<List<Tile>> = listOf()
        repeat (tilemapRows) { row ->
            var newRow: List<Tile> = listOf()
            repeat (tilemapCols) { col ->
                newRow = if (isEdgeCoordinate(Coordinates(col, row))) {
                    newRow.plus(Tile(Coordinates(col, row), TileType.WALL))
                } else {
                    newRow.plus(Tile(Coordinates(col, row), TileType.FLOOR))
                }
            }
            newTilemap = newTilemap.plus(listOf(newRow))
        }
        return newTilemap
    }

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
    fun moveActor(
        actor: Actor,
        movementDirection: MovementDirection
    ) {
        val deltas = movementDeltas[movementDirection]!!
        val targetCoordinates = Coordinates(
            actor.coordinates.x + deltas.dx,
            actor.coordinates.y + deltas.dy
        )
        val targetTile = getTileOrNull(targetCoordinates)
        if (targetTile != null) {
            if (walkableTileType(targetTile) && !tileIsOccupied(targetTile)) {
                var newActorsList = actors.value!!.minus(actor)
                actor.coordinates = targetCoordinates
                newActorsList = newActorsList.plus(actor)
                _actors.value = newActorsList
            }
        }
    }
    fun movePlayerAndProcessTurn(
        movementDirection: MovementDirection
    ) {
        moveActor(getPlayer(), movementDirection)
        incrementTurnsPassed()
        if (cameraCoupled) { snapCameraToPlayer() }
        updateHudStrings()
        updateTilemapStrings()
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
    // TODO: Update the following function or make some other improvement to allow for a
    //  FULL-SCREEN pannable map display...? May be possible. I think it is!
    private fun displayStrings(
        origin: Coordinates,
        ends: Coordinates
    ): List<String> {
        var newDisplayStrings = listOf<String>()
        for (row in origin.y until ends.y) {
            var rowString = ""
            for (col in origin.x until ends.x) {
                val tile = getTileOrNull(Coordinates(col, row))
                rowString += if (tile != null) {
                    if (Coordinates(col, row) in actorCoordinates()) {
                        getActorByCoordinates(Coordinates(col, row)).mapRepresentation
                    } else {
                        when (tile.tileType) {
                            TileType.FLOOR -> "."
                            TileType.WALL -> "#"
                        }
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
        val ends = Coordinates(tilemapCols, tilemapRows)
        _mapScreenStrings.value = displayStrings(origin, ends)
    }

    private var _messageLog = MutableLiveData<List<String>>(listOf())
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

    private fun newPlayer(): Actor {
        return Actor (
            // TODO: Something less arbitrary for the starting spot:
            coordinates = Coordinates(2, 5),
            name = "@player",
            actorFaction = ActorFaction.PLAYER,
            inventory = listOf(
                // TODO: This list is a placeholder.
                healingPotion(),
                healingPotion(),
                healingPotion()
            )
        )
    }

    // TODO: Some factory functions for simple enemies.
    // TODO: Some simple behavior functions for enemy "AI".
    // TODO: Collision detection between Actors, and combat.

    init {
        _tiles.value = when (tilemapType) {
            TilemapType.TESTING -> generateTestingMap()
        }
        _actors.value = _actors.value!!.plus(newPlayer())
        snapCameraToPlayer()
        updateTilemapStrings()
        updateHudStrings()
        addLogMessage("Welcome to Composelike!")
    }
}

class SceneViewModelFactory(
    val tilemapCols: Int,
    val tilemapRows: Int,
    val tilemapType: TilemapType
): ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED CAST")
        return SceneViewModel(tilemapCols, tilemapRows, tilemapType) as T
    }
}