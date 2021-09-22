package com.example.composelike

sealed class Tile(
    val name: String,
    val coordinates: Coordinates,
    val mapRepresentation: String,
    val walkable: Boolean = true,
    var explored: Boolean = false,
    var visible: Boolean = false,
    ) {
    // TODO: Perhaps a variable to track the player's "trail", for some hunting monsters to follow.

    fun isNeighbor(other: Tile): Boolean { return coordinates.isNeighbor(other.coordinates) }

    fun getNeighbors(tiles: List<Tile>): List<Tile> { return tiles.filter { isNeighbor(it) } }

    fun seen(): Tile {
        explored = true
        visible = true
        return this
    }

    fun unSeen(): Tile {
        visible = false
        return this
    }

    class Wall(coordinates: Coordinates) :
        Tile("Wall Tile", coordinates, "#", walkable = false)

    class Floor(coordinates: Coordinates) :
        Tile("Floor Tile", coordinates, " .")

    class StairsUp(coordinates: Coordinates) :
        Tile("Stairs Up", coordinates, "<")

    class StairsDown(coordinates: Coordinates) :
        Tile("Stairs Down", coordinates, ">")
}

fun randomWallOrFloorTile(coordinates: Coordinates): Tile {
    return if (coinFlip()) Tile.Floor(coordinates) else Tile.Wall(coordinates)
}