package com.example.composelike

import android.os.Build
import androidx.annotation.RequiresApi

const val dimensionCap = 80

sealed class RoomSize(val sizeRange: IntRange) {
    fun randomDimension(): Int {
        return sizeRange.random()
    }

    class Small : RoomSize(3..5)
    class Medium : RoomSize(6..9)
    class Large : RoomSize(10..20)
    // More varieties to come (TODO)
}

enum class RoomShape {
    RECTANGLE,
    SPLOTCH,
    ROUND,
    // More varieties to come (TODO)
}

enum class RoomConnectionType {
    DEFAULT,
    // More varieties to come (TODO)
}

/* TODO: In progress.
    Next Up:
        1. It would be a good time to implement a Door tile.
        2. It would be a good time to implement Event Triggers.
        3. The combination of 1 + 2 means that I could cause rooms to "light up" in the
            way of the traditional Rogue while maintaining a modern FOV style in general.
        4. Secret doors, secret hallways, and some rooms replaced with "Mazes" -- all
            features of the original Rogue that would be missing from an homage map.
        5. It would be a good time to look in to a BSP implementation.
 */

sealed class Tilemap(
    initCols: Int,
    initRows: Int,
    private val _parentSimulation: ComposelikeSimulation,
    initTileType: String? = null
) {
    /*
        Dev Note: There is a limit to how big a Tilemap can be before it causes performance
        issues. TODO: Some kind of resource check during map initialization.
     */
    val numCols = if (initCols > dimensionCap) dimensionCap else initCols
    val numRows = if (initRows > dimensionCap) dimensionCap else initRows
    val cols = 0 until numCols
    val rows = 0 until numRows

    val mapRect = MapRect(Coordinates(0, 0), numCols, numRows)
    var lastVisionRect: MapRect? = null

    private var _tiles: MutableList<MutableList<Tile>> = initTiles(initTileType)
    fun tiles(): List<List<Tile>> = _tiles
    fun flattenedTiles(): List<Tile> { return _tiles.flatten() }

    fun getTileOrNull(coordinates: Coordinates): Tile? {
        return _tiles.getOrNull(coordinates.y)?.getOrNull(coordinates.x)
    }

    // TODO: Field of View system is not quite there yet.
    fun setFieldOfView(
        actor: Actor,
        fullMapPass: Boolean = false
    ) {
        val newVisionRect =
           if (fullMapPass || _parentSimulation.debugMode) mapRect else actor.visionRange()
        val overlappingVisionRect =
            if (lastVisionRect == null) newVisionRect else newVisionRect.plus(lastVisionRect!!)
        mapTiles (mapRect = overlappingVisionRect) { tile ->
            if (_parentSimulation.debugMode) tile.seen()
            else if (actor.canSeeTile(tile)) tile.seen()
            else tile.unSeen()
        }
        lastVisionRect = newVisionRect
    }

    private fun isEdgeCoordinate(coordinates: Coordinates): Boolean {
        val col = coordinates.x
        val row = coordinates.y
        return (row == 0 || col == 0 || row == numRows - 1 || col == numCols - 1)
    }

    private fun randomWalkableTile(): Tile {
        return flattenedTiles().filter { it.walkable }.random()
    }

    private fun randomWallTile(): Tile {
        return flattenedTiles().filter { it.tileType == TileType.WALL }.random()
    }

    fun percentWalkable(): Double {
        val walkableTiles = flattenedTiles().filter { it.walkable }.size.toDouble()
        val totalTiles = (numCols * numRows).toDouble()
        return walkableTiles / totalTiles * 100
    }

    /**
     * If initTileType is "wall" or "floor" then it will init the whole Tilemap to that tile type.
     * Otherwise, it randomly picks between Floor and Wall tiles for the whole map.
     */
    private fun initTiles(initTileType: String? = null): MutableList<MutableList<Tile>> {
        val newTilemap = mutableListOf<MutableList<Tile>>()
        repeat (numRows) { row ->
            newTilemap.add(mutableListOf())
            repeat (numCols) { col ->
                val coordinates = Coordinates(col, row)
                newTilemap[row].add(
                    when (initTileType) {
                        "wall" -> Tile.Wall(coordinates)
                        "floor" -> Tile.Floor(coordinates)
                        else -> randomWallOrFloorTile(coordinates)
                    }
                )
            }
        }
        return newTilemap
    }

    /**
     * Applies the desired mapFunction to a subset of the Tilemap.
     */
    private fun mapTiles(
        mapRect: MapRect = this.mapRect,
        mapFunction: (Tile) -> Tile,
    ) {
        mapRect.rows.forEach { row ->
            mapRect.cols.forEach { col ->
                getTileOrNull(Coordinates(col, row))?.let { tile ->
                    _tiles[row][col] = mapFunction(tile)
                }
            }
        }
    }

    /**
     * Returns a new Tilemap as a pure function of the old one, for use primarily with Cellular
     * Automata. It is slower but needed for some kinds of map manipulation.
     */
    fun mappedTiles(
        mapFunction: (Tile) -> Tile,
    ): MutableList<MutableList<Tile>> {
        return _tiles
            .map { row ->
                row.map { tile ->
                    mapFunction(tile)
                }.toMutableList()
            }.toMutableList()
    }

    /**
     * Places Wall tiles around the edges of the map.
     */
    fun withEdgeWalls() {
        mapTiles { tile ->
            if (isEdgeCoordinate(tile.coordinates)) Tile.Wall(tile.coordinates) else tile
        }
    }

    /**
     * Places a random StairsDown Tile on an existing walkable tile.
     */
    fun withRandomStairsDown() {
        val target = randomWalkableTile().coordinates
        _tiles[target.y][target.x] = Tile.StairsDown(target)
    }

    /**
     * Will apply arbitrary Cellular Automata rules to the map for a given number of generations.
     * For now, it only considers Wall or Floor Tiles as potential states. Eventually it will
     * allow for more.
     */
    fun applyCellularAutomata(generations: Int, decisionFunction: (Tile) -> Boolean) {
        repeat (generations) {
            _tiles = mappedTiles { tile ->
                if (decisionFunction(tile)) {
                    Tile.Floor(tile.coordinates)
                } else {
                    Tile.Wall(tile.coordinates)
                }
            }
        }
    }

    fun insertTiles(tiles: List<Tile>) {
        tiles.forEach { tile ->
            _tiles[tile.coordinates.y][tile.coordinates.x] = tile
        }
    }

    /**
     * Will stamp the given room, overriding any other map features which exist at that
     * location. An optional roomNumber allows rooms to be tracked for other purposes later.
     */
    fun stampRoom( // In progress
        roomShape: RoomShape,
        roomSize: RoomSize,
        center: Coordinates,
        roomNumber: Int? = null,
        roomConnectionType: RoomConnectionType = RoomConnectionType.DEFAULT,
    ) : List<ConnectionNode> {
        val connectionNodes = mutableListOf<ConnectionNode>()
        val roomTiles = mutableListOf<Tile>()

        when (roomShape) {
            RoomShape.RECTANGLE -> {
                val roomWidth = roomSize.randomDimension()
                val roomHeight = roomSize.randomDimension()
                val roomOrigin = Coordinates(
                    x = center.x - roomWidth / 2,
                    y = center.y - roomHeight / 2,
                )
                MapRect(roomOrigin, roomWidth, roomHeight).let { roomRect ->
                        roomRect.asCoordinates().forEach { coordinates ->
                        getTileOrNull(coordinates)?.let {
                            roomTiles.add(Tile.Room(coordinates, roomNumber))
                        }
                    }
                }
            }
            RoomShape.ROUND -> {
            } // TODO: In Progress
            RoomShape.SPLOTCH -> {
            } // TODO: In Progress
        }

        val bounds = mapRect.asBounds().withoutEdges()
        when (roomConnectionType) {
            RoomConnectionType.DEFAULT -> connectionNodes.add(
                ConnectionNode.RoomCenter(
                    coordinates = center.bounded(bounds),
                )
            )
            else -> {} // More varieties TODO
        }

        insertTiles(roomTiles)
        return connectionNodes
    }

    /**
     * Generates a dungeon using room accretion, where each room is an offshoot of the previous.
     */
    fun withRoomAccretion(
        roomShapes: List<RoomShape>,
        roomSizes: List<RoomSize>,
        connectionTypes: List<ConnectionPathType>
    ) {
        var roomsStamped = 0

        fun endCondition(): Boolean {
            return percentWalkable() > 50.0 // This will get more complex.
        }

        fun spotRoom(): Coordinates {
            return randomWallTile().coordinates
        }

        var lastNode = stampRoom(
            roomShape = roomShapes.random(),
            roomSize = roomSizes.random(),
            center = spotRoom(),
            roomNumber = roomsStamped,
        ).first()

        while (!endCondition()) {
            roomsStamped++

            val nextNode = stampRoom(
                roomShape = roomShapes.random(),
                roomSize = roomSizes.random(),
                center = spotRoom(),
                roomNumber = roomsStamped,
            ).first()

            nextNode.connect(lastNode, connectionTypes.random(), this)
            lastNode = nextNode
        }
    }

    /**
     * A blank map with walls around the edges. Eventually will include some obstacles and traps.
     */
    class Arena(
        cols: Int = dimensionCap,
        rows: Int = dimensionCap,
        parentSimulation: ComposelikeSimulation
    ) : Tilemap(cols, rows, parentSimulation, "floor") {
        init { withEdgeWalls() }
    }

    /*
        TODO: Conway's Game of Life map! It could run applyCellularAutomata for an arbitrary
            number of setup generations and then yield the resulting Tilemap. Using an event trigger
            system or something I could even cause it to advance generations in game! That's a
            neat idea!
     */

    /**
     * A cave-like map made with a Cellular Automata.
     * This one is a work in progress.
     */
    class Cave(
        cols: Int = dimensionCap,
        rows: Int = dimensionCap,
        parentSimulation: ComposelikeSimulation
    ) : Tilemap(cols, rows, parentSimulation) {
        init {
            applyCellularAutomata(
                /*
                    Recipe Notes:
                        - (generations = 3, neighborThreshold = 5) results in isolated cave "rooms"
                          that will ideally be joined together by hallways.
                        - (generations = 1, neighborThreshold = 4) results in large open rooms
                          with a "cave"-like appearance. It's a good "basic" Cave.
                        - More to come, for sure.
                    TODO: Fine-tune with more parameters and possibly create sub-classes of Cave.
                 */
                generations = 1,
                decisionFunction = { tile ->
                    val neighborThreshold = 4
                    tile.getNeighbors(flattenedTiles())
                        // TODO: Test that this wouldn't be better as a Sequence.
                        .filter { it.walkable }
                        .size >= neighborThreshold
                }
            )
            withEdgeWalls()
            withRandomStairsDown()
            // TODO: Contiguity check.
        }
    }

    /**
     * A "stamped" map with the center being collapsed and cave-like while the perimeter is
     * more reminiscent of a traditional dungeon.
     */
    @RequiresApi(Build.VERSION_CODES.N)
    class CollapsedRuins(
        cols: Int = dimensionCap,
        rows: Int = dimensionCap,
        parentSimulation: ComposelikeSimulation
    ) : Tilemap(cols, rows, parentSimulation, "wall") {
        init {
            withRoomAccretion(
                roomShapes = listOf(RoomShape.RECTANGLE),
                roomSizes = listOf(RoomSize.Small(), RoomSize.Medium(), RoomSize.Large()),
                connectionTypes = listOf(ConnectionPathType.WOBBLE)
            )
            withEdgeWalls()
            withRandomStairsDown()
        }
    }
}