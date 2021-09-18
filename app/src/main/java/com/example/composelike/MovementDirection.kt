package com.example.composelike

sealed class MovementDirection(val dx: Int, val dy: Int) {
    class Up : MovementDirection(0, -1)
    class Down : MovementDirection(0, 1)
    class Left : MovementDirection(-1, 0)
    class Right : MovementDirection(1, 0)
    class UpLeft : MovementDirection(-1, -1)
    class UpRight : MovementDirection(1, -1)
    class DownLeft : MovementDirection(-1, 1)
    class DownRight : MovementDirection(1, 1)
    class Stationary : MovementDirection(0, 0)
}

fun randomMovementDirection(): MovementDirection {
    return listOf(
        MovementDirection.Up(),
        MovementDirection.Down(),
        MovementDirection.Left(),
        MovementDirection.Right(),
        MovementDirection.UpLeft(),
        MovementDirection.UpRight(),
        MovementDirection.DownLeft(),
        MovementDirection.DownRight(),
        MovementDirection.Stationary(),
    ).random()
}