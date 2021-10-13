package com.example.composelike

sealed class Tile(
    val name: String,
    val coordinates: Coordinates,
    private val _mapRepresentation: String,
    val walkable: Boolean = true,
    var explored: Boolean = false,
    var visible: Boolean = false,
    val blocksSightLine: Boolean = false,
    val roomNumber: Int? = null
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

    open fun mapRepresentation(): String {
        return if (visible || explored) _mapRepresentation else " "
    }

    class Wall(coordinates: Coordinates) : Tile(
        name = "Wall Tile",
        coordinates = coordinates,
        _mapRepresentation = "#",
        walkable = false,
        blocksSightLine = true
    )

    class Floor(coordinates: Coordinates) : Tile(
        name = "Floor Tile",
        coordinates = coordinates,
        _mapRepresentation = " ."
    ) {
        override fun mapRepresentation(): String {
            return if (visible) "." else " "
        }
    }

    /**
     * Room Tiles are marked with their roomNumber in order to make it easier to treat them
     * as groups during map generation and (eventually) during event triggers.
     */
    class Room(coordinates: Coordinates, roomNumber: Int) : Tile(
        name = "Room Tile",
        coordinates = coordinates,
        _mapRepresentation = " .",
        roomNumber = roomNumber
    ) {
        override fun mapRepresentation(): String {
            return if (visible) "." else " "
        }
    }

    class Door(coordinates: Coordinates) : Tile(
        name = "Door Tile",
        coordinates = coordinates,
        _mapRepresentation = "+." // <-- Not positive this will be ideal yet.
        // TODO: Questions on how to make this interact with FOV in the best way. Probably
        //  having OpenDoor and ClosedDoor and making them switch with an opened() / closed() pair
        //  of functions, similar to seen() and unSeen().
        // TODO: Locked doors and Event Triggers.
    )

    class StairsUp(coordinates: Coordinates) : Tile(
        name = "Stairs Up",
        coordinates = coordinates,
        _mapRepresentation = "<"
    )

    class StairsDown(coordinates: Coordinates) : Tile(
        name = "Stairs Down",
        coordinates = coordinates,
        _mapRepresentation = ">"
    )
}

fun randomWallOrFloorTile(coordinates: Coordinates): Tile {
    return if (coinFlip()) Tile.Floor(coordinates) else Tile.Wall(coordinates)
}