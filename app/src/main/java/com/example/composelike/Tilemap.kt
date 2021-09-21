package com.example.composelike

sealed class Tilemap(
    val cols: Int,
    val rows: Int,
    initTileType: String? = null,
) {
    private var _tiles: List<List<Tile>> = initTiles(initTileType)

    fun tiles(): List<Tile> { return _tiles.flatten() }

    fun getTileOrNull(coordinates: Coordinates): Tile? {
        return _tiles.getOrNull(coordinates.y)?.getOrNull(coordinates.x)
    }

    fun isEdgeCoordinate(coordinates: Coordinates): Boolean {
        val col = coordinates.x
        val row = coordinates.y
        return (row == 0 || col == 0 || row == rows - 1 || col == cols - 1)
    }

    private fun randomWalkableTile(): Tile {
        return tiles().filter { it.walkable }.random()
    }

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
    fun mappedTiles(mapFunction: (Tile) -> Tile): List<List<Tile>> {
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
    class Testing(cols: Int, rows: Int) : Tilemap(cols, rows, "floor") {
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

    /**
     * A cave-like map made with a Cellular Automata.
     */
    class Cave(cols: Int, rows: Int) : Tilemap(cols, rows) {
        init {
            applyCellularAutomata(
                // TODO: This can be fine-tuned a little more.
                generations = 1,
                decisionFunction = { tile ->
                    val neighborThreshold = 5
                    tile.getNeighbors(tiles())
                        .filter { it.walkable }
                        .size >= neighborThreshold
                }
            )
            withEdgeWalls()
            withRandomStairsDown()
            // TODO: Contiguity check.
        }
    }

    //class ClassicDungeon // TODO
}