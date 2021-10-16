package com.example.composelike

import android.os.Build
import androidx.annotation.RequiresApi
import java.lang.Math.min

const val dimensionCap = 80

data class MapRect(val origin: Coordinates, val width: Int, val height: Int) {
    val cols = origin.x until (origin.x + width)
    val rows = origin.y until (origin.y + height)

    fun asCoordinates(): List<Coordinates> {
        var coordinatesList = listOf(origin)
        rows.forEach { row ->
            cols.forEach { col ->
                coordinatesList = coordinatesList.plus(
                    Coordinates(origin.x + col, origin.y + row)
                )
            }
        }
        return coordinatesList
    }

    fun contains(coordinates: Coordinates): Boolean {
        return asCoordinates().contains(coordinates)
    }

    fun plus(other: MapRect): MapRect {
        return asCoordinates()
            .plus(other.asCoordinates())
            .let { allCoordinates ->
            MapRect(
                origin = Coordinates(
                    x = min(origin.x, other.origin.x),
                    y = min(origin.y, other.origin.y)
                ),
                width = allCoordinates
                    .map { it.x }
                    .maxOrNull()!!
                    .minus(origin.x),
                height = allCoordinates
                    .map { it.y }
                    .maxOrNull()!!
                    .minus(origin.y)
            )
        }
    }
}

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
            else if (actor.canSeeTile(tile, _parentSimulation)) tile.seen()
            else tile.unSeen()
        }
        lastVisionRect = newVisionRect
    }

    private fun isEdgeCoordinate(coordinates: Coordinates): Boolean {
        val col = coordinates.x
        val row = coordinates.y
        return (row == 0 || col == 0 || row == numRows - 1 || col == numCols - 1)
    }

    private fun randomWalkableTile(): Tile { return flattenedTiles().filter { it.walkable }.random() }

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
     * A blank map with walls around the edges.
     */
    class Testing(
        cols: Int = dimensionCap,
        rows: Int = dimensionCap,
        parentSimulation: ComposelikeSimulation
    ) : Tilemap(cols, rows, parentSimulation, "floor") {
        init { withEdgeWalls() }
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

    /*
        TODO: Conway's Game of Life map! It could run applyCellularAutomata for an arbitrary
            number of setup generations and then yield the resulting Tilemap. Using an event trigger
            system or something I could even cause it to advance generations in game! That's a
            neat idea!
     */

    /**
     * A cave-like map made with a Cellular Automata.
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

    private fun insertTiles(tiles: List<Tile>) {
        tiles.forEach { tile ->
            _tiles[tile.coordinates.y][tile.coordinates.x] = tile
        }
    }

    /**
     * This function assumes that the Tilemap has been initialized to all or mostly Wall tiles.
     * It will default to "stamping" rooms in a grid-like manner for now, but eventually it will
     * do more interesting things.
     */
    fun withStampedRoomsGrid(
        roomShape: String = "rectangular",
        roomSizeRange: IntRange = 4..6,
        roomSpacing: IntRange = 3..9
    ): List<Coordinates> {
        // TODO: Different room shapes.
        val nodesList = mutableListOf<Coordinates>()
        var roomsStamped = 0
        var currentRoomTopLeft = Coordinates(1, 1)
        var currentRoomWidth = roomSizeRange.random()
        var currentRoomHeight = roomSizeRange.random()
        val roomTiles = mutableListOf<Tile>()

        fun stamping(): Boolean {
            return currentRoomTopLeft.y + roomSizeRange.last + 1 < numRows - 1
        }

        fun stampRoom() {
            repeat (currentRoomHeight) { row ->
                repeat (currentRoomWidth) { col ->
                    roomTiles.add(
                        Tile.Room(
                            Coordinates(
                                x = currentRoomTopLeft.x + col,
                                y = currentRoomTopLeft.y + row
                            ),
                            roomsStamped
                        )
                    )
                }
            }
        }

        fun addNode() {
            nodesList.add(
                Coordinates(
                    x = currentRoomTopLeft.x + currentRoomWidth / 2,
                    y = currentRoomTopLeft.y + currentRoomHeight / 2
                )
            )
        }

        fun setupNextRoom() {
            roomsStamped++
            val newRoomWidth = roomSizeRange.random()
            val newRoomHeight = roomSizeRange.random()
            val spacing = roomSpacing.random()
            var nextTopLeft = Coordinates(
                x = if (currentRoomTopLeft.x + currentRoomWidth + spacing + newRoomWidth < numCols - 1)
                    currentRoomTopLeft.x + currentRoomWidth + spacing
                else 1,
                y = currentRoomTopLeft.y
            )

            if (nextTopLeft.x < currentRoomTopLeft.x) {
                nextTopLeft = Coordinates(
                    x = nextTopLeft.x,
                    y = nextTopLeft.y + roomSizeRange.last + 1
                )
            }

            currentRoomTopLeft = nextTopLeft
            currentRoomHeight = newRoomHeight
            currentRoomWidth = newRoomWidth
        }

        while (stamping()) {
            stampRoom()
            addNode()
            setupNextRoom()
        }

        insertTiles(roomTiles)

        return nodesList
    }

    /**
     * Runs a series of A* paths between the nodes in the nodesList and carves out hallways.
     * Will not override Room Tiles. Will use waypoints and as-yet-undetermined fuzziness and
     * logic in order to connect the nodes in a neat way.
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun withConnectedRooms(nodesList: List<Coordinates>) {
        // TODO: This was a placeholder draft. Time to make real halls.

        val shuffledNodes = nodesList.shuffled() as MutableList<Coordinates>
        val hallTiles = mutableListOf<Tile>()
        var previous = shuffledNodes.removeFirst()

        while (shuffledNodes.isNotEmpty()) {
            shuffledNodes.removeFirstOrNull()?.let { node ->
                node.shortestPathTo(
                    goal = previous,
                    xBound = numCols,
                    yBound = numRows,
                    simulation = _parentSimulation,
                    heuristicFunction = { node, _, _ ->
                        node.euclideanDistance(previous)
                    }
                )?.let { path ->
                    path.forEach { coordinates ->
                        getTileOrNull(coordinates)?.let { tile ->
                            if (tile.name == "Wall Tile")
                                hallTiles.add(Tile.Floor(coordinates))
                        }
                    }
                }
                previous = node
            }
        }
        insertTiles(hallTiles)
    }

    /**
     * Will be a simple "rectangular rooms and twisty hallways" dungeon, with elements reminiscent
     * of traditional Roguelikes.
     */
    @RequiresApi(Build.VERSION_CODES.N)
    class ClassicDungeon(
        cols: Int = dimensionCap,
        rows: Int = dimensionCap,
        parentSimulation: ComposelikeSimulation
    ) : Tilemap(cols, rows, parentSimulation, "wall") {
        /*
            pcode:
                1. Place rooms by row in grid-like arrangement, perhaps using a re-usable
                    room-stamping function (as this is far from the only map type which will
                    use it).
                    1a. At the center (roughly) of each Room, place a node in a nodesList of
                        some kind.

                2. Connect all the nodes in the nodesList. It could be randomly, it could be
                    according to some logic, and waypoints can be injected in to an A* path to
                    create sensible hallways that still curve in a natural way.

            Extras:
                1. It would be a good time to implement a Door tile.
                2. It would be a good time to implement Event Triggers.
                3. The combination of 1 + 2 means that I could cause rooms to "light up" in the
                    way of the traditional Rogue while maintaining a modern FOV style in general.
                4. Secret doors, secret hallways, and some rooms replaced with "Mazes" -- all
                    features of the original Rogue that would be missing from an homage map.
                5. It would be a good time to look in to a BSP implementation.

            Status: Rough draft complete. Most of the Extras remain to be tackled, but all
                look feasible at this point. Excellent stuff!
         */
        init {
            withConnectedRooms(withStampedRoomsGrid())
            // TODO: Refinement: ^ These two functions are in the early stages and should become
            //  much more complex and interesting soon.

            withEdgeWalls()
            withRandomStairsDown()
        }
    }
}