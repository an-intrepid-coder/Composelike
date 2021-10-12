package com.example.composelike

import android.os.Build
import androidx.annotation.RequiresApi

const val dimensionCap = 80 // <-- This will increase with optimizations.

data class MapRect(val origin: Coordinates, val width: Int, val height: Int) {
    fun contains(target: Coordinates): Boolean {
        return target.x >= origin.x &&
                target.x < origin.x + width &&
                target.y >= origin.y &&
                target.y < origin.y + height
    }

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
    val cols = if (initCols > dimensionCap) dimensionCap else initCols
    val rows = if (initRows > dimensionCap) dimensionCap else initRows

    val mapRect = MapRect(Coordinates(0, 0), cols, rows)

    private var _tiles: MutableList<MutableList<Tile>> = initTiles(initTileType)
    fun tiles(): List<Tile> { return _tiles.flatten() }

    fun getTileOrNull(coordinates: Coordinates): Tile? {
        return _tiles.getOrNull(coordinates.y)?.getOrNull(coordinates.x)
    }

    fun setFieldOfView(actor: Actor) {
        // TODO: Optimize
        val range = if (_parentSimulation.debugMode) mapRect else actor.visionRange()
        mapTiles(rect = range) { tile ->
            if (_parentSimulation.debugMode) tile.seen()
            else if (actor.canSeeTile(tile, _parentSimulation)) tile.seen()
            else tile.unSeen()
        }
    }

    private fun isEdgeCoordinate(coordinates: Coordinates): Boolean {
        val col = coordinates.x
        val row = coordinates.y
        return (row == 0 || col == 0 || row == rows - 1 || col == cols - 1)
    }

    private fun randomWalkableTile(): Tile { return tiles().filter { it.walkable }.random() }

    /**
     * If initTileType is "wall" or "floor" then it will init the whole Tilemap to that tile type.
     * Otherwise, it randomly picks between Floor and Wall tiles for the whole map.
     */
    private fun initTiles(initTileType: String? = null): MutableList<MutableList<Tile>> {
        val newTilemap = mutableListOf<MutableList<Tile>>()
        repeat (rows) { row ->
            newTilemap.add(mutableListOf())
            repeat (cols) { col ->
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
        rect: MapRect = this.mapRect,
        mapFunction: (Tile) -> Tile,
    ) {
        _tiles.apply {
            rect.rows.forEach { row ->
                rect.cols.forEach { col ->
                    if (mapRect.contains(Coordinates(col, row)))
                        this[row][col] = mapFunction(this[row][col])
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
                    tile.getNeighbors(tiles())
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
    fun withStampedRooms(): List<Coordinates> {
        val roomSizeRange = 4..6
        val roomSpacing = 3..9

        val nodesList = mutableListOf<Coordinates>()
        var roomsStamped = 0
        var currentRoomTopLeft = Coordinates(1, 1)
        var currentRoomWidth = roomSizeRange.random()
        var currentRoomHeight = roomSizeRange.random()

        val roomTiles = mutableListOf<Tile>()

        while (currentRoomTopLeft.y + roomSizeRange.last + 1 < rows - 1) {

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

            nodesList.add(
                Coordinates(
                    x = currentRoomTopLeft.x + currentRoomWidth / 2,
                    y = currentRoomTopLeft.y + currentRoomHeight / 2
                )
            )

            roomsStamped++
            val newRoomWidth = roomSizeRange.random()
            val newRoomHeight = roomSizeRange.random()
            val spacing = roomSpacing.random()
            var nextTopLeft = Coordinates(
                x = if (currentRoomTopLeft.x + currentRoomWidth + spacing + newRoomWidth < cols - 1)
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

        insertTiles(roomTiles)

        return nodesList
    }

    /**
     * Runs a series of A* paths between the nodes in the nodesList and carves out hallways.
     * Will not override Room Tiles. Will use waypoints and as-yet-undetermined fuzziness and
     * logic in order to connect the nodes in a neat way. <-- TODO
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun withConnectedRooms(nodesList: List<Coordinates>) {
        // TODO: A more traditional arrangement of room connections, via some mathematical
        //  formula. I bet I can think of something.
        val shuffledNodes = nodesList.shuffled() as MutableList<Coordinates>

        val hallTiles = mutableListOf<Tile>()

        var previous = shuffledNodes.removeFirst()

        while (shuffledNodes.isNotEmpty()) {
            shuffledNodes.removeFirstOrNull()?.let { node ->
                node.shortestPathTo(
                    goal = previous,
                    xBound = cols,
                    yBound = rows,
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
            withConnectedRooms(withStampedRooms())
            // TODO: Refinement: ^ These two functions are in the early stages and should become
            //  much more complex and interesting soon.

            withEdgeWalls()
            withRandomStairsDown()
        }
    }
}