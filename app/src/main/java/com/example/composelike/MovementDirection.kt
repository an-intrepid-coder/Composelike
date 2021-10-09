package com.example.composelike

sealed class MovementDirection(val name: String, val dx: Int, val dy: Int) {
    fun repr(): String {
        return "$name (dx = $dx, dy = $dy)"
    }

    class Raw(dx: Int, dy: Int) : MovementDirection("Raw", dx, dy)
    class Up : MovementDirection("Up", 0, -1)
    class Down : MovementDirection("Down", 0, 1)
    class Left : MovementDirection("Left", -1, 0)
    class Right : MovementDirection("Right", 1, 0)
    class UpLeft : MovementDirection("UpLeft", -1, -1)
    class UpRight : MovementDirection("UpRight", 1, -1)
    class DownLeft : MovementDirection("DownLeft", -1, 1)
    class DownRight : MovementDirection("DownRight", 1, 1)
    class Stationary : MovementDirection("Stationary", 0, 0)
}

val allMovementDirections = listOf(
    MovementDirection.Up(),
    MovementDirection.Down(),
    MovementDirection.Left(),
    MovementDirection.Right(),
    MovementDirection.UpLeft(),
    MovementDirection.UpRight(),
    MovementDirection.DownLeft(),
    MovementDirection.DownRight(),
    MovementDirection.Stationary(),
)

fun randomMovementDirection(): MovementDirection { return allMovementDirections.random() }