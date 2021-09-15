package com.example.composelike

enum class ActorFaction {
    PLAYER,
    ENEMY,
    // more to come
}

open class Actor(
    var coordinates: Coordinates,
    val name: String,
    val actorFaction: ActorFaction = ActorFaction.ENEMY
) {
    val mapRepresentation = name[0]
}

// TODO: Player & other sub-classes.