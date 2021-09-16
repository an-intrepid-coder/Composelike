package com.example.composelike

enum class ActorFaction {
    PLAYER,
    NEUTRAL,
    ENEMY,
    // more to come
}

fun newPlayer(coordinates: Coordinates): Actor {
    return Actor (
        coordinates = coordinates,
        name = "@player",
        actorFaction = ActorFaction.PLAYER,
        inventory = listOf(
            // This list is a placeholder.
            healingPotion(),
            healingPotion(),
            healingPotion()
        )
    )
}

fun weakGoblin(coordinates: Coordinates): Actor {
    val goblin = Actor (
        coordinates = coordinates,
        name = "goblin",
        actorFaction = ActorFaction.ENEMY,
        maxHealth = 3,
        maxMana = 1,
        behaviorType = BehaviorType.SIMPLE_ENEMY,
        // TODO: An inventory with some loot!
    )
    return goblin
}

enum class BehaviorType {
    NONE,
    WANDERING,
    SIMPLE_ENEMY,
}

open class Actor(
    // TODO: A class and leveling system! Vaguely DnD-like, for now.
    var coordinates: Coordinates,
    val name: String,
    val actorFaction: ActorFaction = ActorFaction.NEUTRAL,
    var maxHealth: Int = 8,
    var maxMana: Int = 3,
    var bonusAttack: Int = 0,
    var bonusDefense: Int = 0,
    var gold: Int = 0,
    var level: Int = 1,
    var experienceToLevel: Int = 1000,
    var inventory: List<Item> = listOf(),
    var behaviorType: BehaviorType = BehaviorType.NONE,
) {
    var health = maxHealth
    var mana = maxMana
    val mapRepresentation = name[0]

    fun addItem(item: Item) {
        inventory = inventory.plus(item)
    }

    fun removeItem(item: Item) {
        inventory = inventory.minus(item)
    }

    fun isAlive(): Boolean { return health > 0 }

    fun rewardXp(xp: Int) { experienceToLevel -= xp }

    fun neighboringActors(actorList: List<Actor>): List<Actor> {
        return actorList.filter { coordinates.isNeighbor(it.coordinates) }
    }

    fun isNeighbor(other: Actor): Boolean {
        return coordinates.isNeighbor(other.coordinates)
    }
}