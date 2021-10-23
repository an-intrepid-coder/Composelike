package com.example.composelike

enum class ConnectionPathType {
    DIRECT,
    ELBOW,
    WANDERING,
    MAZE,
}

/**
 * The faces of a connection node determine the "directions" in which it will try to expand while
 * building hallways during room accretion.
 */
sealed class ConnectionNode(
    val coordinates: Coordinates,
    val faces: List<MovementDirection>, // Will make use of this for more complex map types
) {
    // TODO: Working for simple stuff but more complex path-types remain to be attempted.

    /**
     * Connects two ConnectionNodes in a variety of potential ways.
     * TODO: This may deserve to be its own class. Must look into it.
     */
    open fun connect(
        other: ConnectionNode,
        connectionPathType: ConnectionPathType,
        tilemap: Tilemap,
    ) {
        fun validPlacement(tile: Tile): Boolean {
            return tile.tileType == TileType.WALL
            // More complex conditions in the future.
        }

        val newHallTiles = mutableListOf<Tile>()
        when (connectionPathType) {
            ConnectionPathType.DIRECT -> {
                AStarPath.Direct(
                    start = coordinates,
                    goal = other.coordinates,
                    bounds = tilemap.mapRect.asBounds()
                ).path?.forEach { coordinates ->
                    tilemap.getTileOrNull(coordinates)?.let { tile ->
                        if (validPlacement(tile)) {
                            newHallTiles.add(Tile.Floor(coordinates))
                        }
                    }
                }
            }
            ConnectionPathType.ELBOW -> {
                // Will have one or perhaps two "turns" inserted as waypoints.
            } // TODO
            ConnectionPathType.WANDERING -> {
                // Will do a drunken walk with semi-random bounds.
            } // TODO
            ConnectionPathType.MAZE -> {
                // The most challenging: This will create a full-on maze with exit and entrance
                //  being the two connecting nodes.
            } // TODO
        }
        tilemap.insertTiles(newHallTiles)
    }

    /**
     * RoomCenter is a more primitive type of ConnectionNode which is used for simpler map
     * accretion and some other special cases.
     */
    class RoomCenter(
        coordinates: Coordinates,
    ) : ConnectionNode(
        coordinates = coordinates,
        faces = allMovementDirections.minus(MovementDirection.Stationary()),
    )

    // TODO: More complex ConnectionNode types which project new hallways and rooms in
    //  specific combinations of directions with cues to generate specific kinds of
    //  features along the way.
}