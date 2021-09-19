package com.example.composelike

sealed class Tile(
    val coordinates: Coordinates,
    val mapRepresentation: String,
    val walkable: Boolean = true,
    val explored: Boolean = false,
    val visible: Boolean = false,
    // TODO: Perhaps a variable to track the player's "trail", for some hunting monsters to follow.
    ) {

    fun isNeighbor(other: Tile): Boolean { return coordinates.isNeighbor(other.coordinates) }

    fun getNeighbors(tiles: List<Tile>): List<Tile> { return tiles.filter { isNeighbor(it) } }

    class Wall(coordinates: Coordinates) : Tile(coordinates, "#", walkable = false)
    class Floor(coordinates: Coordinates) : Tile(coordinates, ".")
    class StairsUp(coordinates: Coordinates) : Tile(coordinates, "<")
    class StairsDown(coordinates: Coordinates) : Tile(coordinates, ">")
}

fun randomWallOrFloorTile(coordinates: Coordinates): Tile {
    return if (coinFlip()) Tile.Floor(coordinates) else Tile.Wall(coordinates)
}