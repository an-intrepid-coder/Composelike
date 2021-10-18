package com.example.composelike

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

    fun asBounds(): Bounds { return Bounds(width, height) }

    fun contains(coordinates: Coordinates): Boolean {
        return asCoordinates().contains(coordinates)
    }

    fun plus(other: MapRect): MapRect {
        return asCoordinates()
            .plus(other.asCoordinates())
            .let { allCoordinates ->
                MapRect(
                    origin = Coordinates(
                        x = origin.x.coerceAtMost(other.origin.x),
                        y = origin.y.coerceAtMost(other.origin.y)
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