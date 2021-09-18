package com.example.composelike

// TODO: Turn this into a sealed class!

enum class TileType {
    WALL,
    FLOOR,
    STAIRS_UP,
    STAIRS_DOWN,
    // more to come
}

data class Tile(
    val coordinates: Coordinates,
    val tileType: TileType,
    val explored: Boolean = false,
    val visible: Boolean = false,
    // TODO: Perhaps a variable to track the player's "trail", for some hunting monsters to follow.
    ) {

    fun isNeighbor(other: Tile): Boolean {
        return coordinates.isNeighbor(other.coordinates)
    }

    fun getNeighbors(tilemap: List<List<Tile>>): List<Tile> {
        return tilemap.flatten().filter { isNeighbor(it) }
    }

    /**
     * Returns true if the given tile can be walked on.
     */
    fun isWalkable(): Boolean {
        return when (tileType) {
            TileType.WALL -> false
            else -> true
        }
        // This will grow down the road.
    }
}