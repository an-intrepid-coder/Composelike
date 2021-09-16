package com.example.composelike

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlin.system.exitProcess

enum class TileType {
    WALL,
    FLOOR
    // more to come
}

enum class TilemapType {
    TESTING,
    CAVE,
    CLASSIC_DUNGEON,
    // more to come
}

data class Tile(val coordinates: Coordinates, val tileType: TileType) {
    fun isNeighbor(other: Tile): Boolean {
        return coordinates.isNeighbor(other.coordinates)
    }
    fun getNeighbors(tilemap: List<List<Tile>>): List<Tile> {
        return tilemap.flatten().filter { isNeighbor(it) }
    }
}

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

// TODO: Long-Term: Replace tilemapStrings and mapScreenStrings with List<List<Cell>> and
//  implement technicolor display logic. Monochrome until then.

class GameViewModel(
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

    /**
     * Returns true if the given tile can be walked on.
     * // TODO: Make this a member function of Tile.
     */
    fun walkableTileType(tile: Tile): Boolean {
        return when (tile.tileType) {
            TileType.WALL -> false
            else -> true
        }
        // This will grow down the road.
    }

    /**
     * Returns true if the given coordinates are on the edge of the tilemap.
     */
    fun isEdgeCoordinate(coordinates: Coordinates): Boolean {
        val col = coordinates.x
        val row = coordinates.y
        return (row == 0 || col == 0 || row == tilemapRows - 1 || col == tilemapCols - 1)
    }

    /**
     * Generates a big empty room with wall tiles around the edges.
     */
    private fun generateTestingMap(): List<List<Tile>> {
        return withEdgeWalls(initTilemap(TileType.FLOOR))
    }

    /**
     * Returns a new tilemap by mapping mapFunction to each tile on the given tilemap.
     */
    fun mapTilemapByTile(
        tilemap: List<List<Tile>>,
        mapFunction: (Tile) -> Tile,
    ): List<List<Tile>> {
        var newTilemap: List<List<Tile>> = listOf()
        repeat (tilemapRows) { row ->
            var newRow: List<Tile> = listOf()
            repeat (tilemapCols) { col ->
                newRow = newRow.plus(mapFunction(tilemap[row][col]))
            }
            newTilemap = newTilemap.plus(listOf(newRow))
        }
        return newTilemap
    }

    /**
     * Initializes a blank map with all tiles set to initTileType. If no initTileType is provided
     * then it randomly chooses between FLOOR and WALL for each tile (for now).
     */
    fun initTilemap(initTileType: TileType? = null): List<List<Tile>> {
        fun randomWallOrFloor(): TileType {
            return if ((0..1).random() == 1) TileType.FLOOR else TileType.WALL
        }
        var newTilemap: List<List<Tile>> = listOf()
        repeat (tilemapRows) { row ->
            var newRow: List<Tile> = listOf()
            repeat (tilemapCols) { col ->
                newRow = newRow.plus(
                    Tile(
                        coordinates = Coordinates(col, row),
                        tileType = initTileType ?: randomWallOrFloor()
                    )
                )
            }
            newTilemap = newTilemap.plus(listOf(newRow))
        }
        return newTilemap
    }

    /**
     * Returns the given tilemap with WALL tiles around the edges.
     */
    fun withEdgeWalls(tilemap: List<List<Tile>>): List<List<Tile>> {
        return mapTilemapByTile(tilemap) {
            if (isEdgeCoordinate(it.coordinates)) {
                Tile(it.coordinates, TileType.WALL)
            } else {
                it
            }
        }
    }

    /**
     * Uses cellular automata to create a cave-like map.
     */
    private fun generateCaveTilemap(): List<List<Tile>> {
        /*
            Recipe Notes:
                - A higher neighbor threshold requires more passes to "smooth out" and "open up".
                - TODO: Contiguity checking.
                - TODO: Some more parameters.
                - TODO: Exits and Entrances!
         */
        val neighborThreshold = 4
        val numPasses = 2

        var newTilemap: List<List<Tile>> = initTilemap()

        /**
         * Returns true if the given Tile has enough neighboring tiles which are FLOOR tiles.
         */
        fun tileLives(tile: Tile): Boolean {
            if (tile.tileType == TileType.FLOOR) return true
            val numNeighboringFloorTiles = tile.getNeighbors(newTilemap).filter {
                it.tileType == TileType.FLOOR
            }.size
            return numNeighboringFloorTiles >= neighborThreshold
        }

        /**
         * Returns a new tilemap as a pure function of the old one.
         */
        fun generationPass(tilemap: List<List<Tile>>): List<List<Tile>> {
            return mapTilemapByTile(
                tilemap = tilemap,
                mapFunction = {
                    if (tileLives(it)) {
                        Tile(it.coordinates, TileType.FLOOR)
                    } else {
                        Tile(it.coordinates, TileType.WALL)
                    }
                }
            )
        }

        repeat (numPasses) {
            newTilemap = generationPass(newTilemap)
        }
        return withEdgeWalls(newTilemap)
    }

    /**
     * Generates a simple "Rooms and Corridors" map reminiscent of Rogue or Nethack.
     */
    private fun generateClassicDungeonTilemap(): List<List<Tile>> {
        return listOf() // TODO
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

    fun moveActor(actor: Actor, movementDirection: MovementDirection) {
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
     * Returns a list of Coordinates at which an Actor could spawn (unoccupied FLOOR tiles).
     */
    fun validSpawnCoordinates(): List<Coordinates> {
        return tiles.value!!.flatten().filter {
            !actorCoordinates().contains(it.coordinates) && it.tileType == TileType.FLOOR
        }.map { it.coordinates }
    }

    /**
     * For now, generates a goblin for every 30 FLOOR tiles. Tentative.
     */
    fun generateSmallGoblinPopulation() {
        val numGoblins = tiles.value!!.flatten()
            .filter { it.tileType == TileType.FLOOR }
            .size / 30
        repeat (numGoblins) {
            val spawnCoordinates = validSpawnCoordinates()
            if (spawnCoordinates.isNotEmpty()) {
                _actors.value = _actors.value!!.plus(weakGoblin(
                    coordinates = spawnCoordinates.random()
                ))
            }
        }
    }

    init {
        _tiles.value = when (tilemapType) {
            TilemapType.TESTING -> generateTestingMap()
            TilemapType.CAVE -> generateCaveTilemap()
            TilemapType.CLASSIC_DUNGEON -> generateClassicDungeonTilemap()
        }
        _actors.value = _actors.value!!.plus(newPlayer(
            coordinates = validSpawnCoordinates().random()
        ))
        generateSmallGoblinPopulation()
        snapCameraToPlayer()
        updateTilemapStrings()
        updateHudStrings()
        addLogMessage("Welcome to Composelike!")
        addLogMessage("You must find the Orb of Victory.")
        addLogMessage("It is somewhere deep below...")
    }

    /**
     * Will wander in a random direction, if possible. Will not attack.
     */
    fun wanderingBehavior(actor: Actor) { moveActor(actor, MovementDirection.values().random()) }

    /**
     * If an Actor of a hostile faction is adjacent, the Actor will attack. Otherwise, it will
     * wander randomly.
     */
    fun simpleEnemyBehavior(actor: Actor) {
        if (getPlayer().isNeighbor(actor, actors.value!!)) {
            actorsFight(actor, getPlayer())
        } else {
            wanderingBehavior(actor)
        }
    }
}

class SceneViewModelFactory(
    val tilemapCols: Int,
    val tilemapRows: Int,
    val tilemapType: TilemapType
): ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED CAST")
        return GameViewModel(tilemapCols, tilemapRows, tilemapType) as T
    }
}