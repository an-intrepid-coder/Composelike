package com.example.composelike

enum class ActorFaction {
    PLAYER,
    NEUTRAL,
    ENEMY,
    // more to come
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
    var inventory: List<Item> = listOf()
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
}