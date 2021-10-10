package com.example.composelike

import android.os.Build
import androidx.annotation.RequiresApi

sealed class Tilemap(
    initCols: Int,
    initRows: Int,
    private val _parentSimulation: ComposelikeSimulation,
    initTileType: String? = null
) {
    private val _dimensionCap = 40
    /*
        Dev Note: There is a limit to how big a Tilemap can be before it causes performance
        issues. TODO: Some kind of resource check during map initialization.

        This is only an issue for very large maps (greater than 100x100) but it would be nice
        to have an optimized solution down the road which can handle such maps. For now, 40x40
        is a very safe, practical, and performant cap.

        TODO: Optimization: This class can probably be optimized a lot.
     */
    val cols = if (initCols > _dimensionCap) _dimensionCap else initCols
    val rows = if (initRows > _dimensionCap) _dimensionCap else initRows

    private var _tiles: List<List<Tile>> = initTiles(initTileType)

    fun tiles(): List<Tile> { return _tiles.flatten() }

    fun getTileOrNull(coordinates: Coordinates): Tile? {
        return _tiles.getOrNull(coordinates.y)?.getOrNull(coordinates.x)
    }

    fun setFieldOfView(actor: Actor) {
        _tiles = mappedTiles { tile ->
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
    private fun initTiles(initTileType: String? = null): List<List<Tile>> {
        var newTilemap: List<List<Tile>> = listOf()
        repeat (rows) { row ->
            var newRow: List<Tile> = listOf()
            repeat (cols) { col ->
                val coordinates = Coordinates(col, row)
                newRow = newRow.plus(
                    when (initTileType) {
                        "wall" -> Tile.Wall(coordinates)
                        "floor" -> Tile.Floor(coordinates)
                        else -> randomWallOrFloorTile(coordinates)
                    }
                )
            }
            newTilemap = newTilemap.plus(listOf(newRow))
        }
        return newTilemap
    }

    /**
     * Returns new tiles by mapping mapFunction to each tile.
     */
    private fun mappedTiles(mapFunction: (Tile) -> Tile): List<List<Tile>> {
        /*
            TODO: Optimization: This function is the backbone of the map generation process and
                also runs every turn to set the Field of View. It can probably be heavily optimized
                using Sequences. <--- Next up, I think.
         */
        var newTilemap: List<List<Tile>> = listOf()
        repeat (rows) { row ->
            var newRow: List<Tile> = listOf()
            repeat (cols) { col ->
                newRow = newRow.plus(mapFunction(_tiles[row][col]))
            }
            newTilemap = newTilemap.plus(listOf(newRow))
        }
        return newTilemap
    }

    /**
     * Places Wall tiles around the edges of the map.
     */
    fun withEdgeWalls() {
        _tiles = mappedTiles { tile ->
            if (isEdgeCoordinate(tile.coordinates)) Tile.Wall(tile.coordinates) else tile
        }
    }

    /**
     * Places a random StairsDown Tile on an existing walkable tile.
     */
    fun withRandomStairsDown() {
        val targetTile = randomWalkableTile()
        _tiles = mappedTiles { tile ->
            if (tile == targetTile) Tile.StairsDown(targetTile.coordinates) else tile
        }
    }

    /**
     * A blank map with walls around the edges.
     */
    class Testing(
        cols: Int,
        rows: Int,
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
        cols: Int,
        rows: Int,
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
        _tiles = mappedTiles { tile ->
            if (tile.coordinates in tiles.map { it.coordinates })
                tiles.first { it.coordinates == tile.coordinates }
            else tile
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
        // TODO: Waypoints!
        val shuffledNodes = nodesList.shuffled() as MutableList<Coordinates>

        val hallTiles = mutableListOf<Tile>()

        var previous = shuffledNodes.removeFirst()

        while (shuffledNodes.isNotEmpty()) {
            shuffledNodes.removeFirstOrNull()?.let {
                it.shortestPathTo(
                    goal = previous,
                    xBound = cols,
                    yBound = rows,
                    simulation = _parentSimulation,
                    heuristicFunction = { node, actor, simulation ->
                        node.euclideanDistance(previous)
                    }
                )?.let { path ->
                    for (coordinates in path) {
                        getTileOrNull(coordinates)?.let { tile ->
                            if (tile.name == "Wall Tile") hallTiles.add(Tile.Floor(coordinates))
                        }
                    }
                }
                previous = it
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
        cols: Int,
        rows: Int,
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