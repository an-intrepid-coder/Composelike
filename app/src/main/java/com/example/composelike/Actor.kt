package com.example.composelike

import android.os.Build
import androidx.annotation.RequiresApi

enum class ActorFaction {
    PLAYER,
    NEUTRAL,
    ENEMY,
    // more to come
}

sealed class Actor(
    var coordinates: Coordinates,
    val name: String,
    val actorFaction: ActorFaction = ActorFaction.NEUTRAL,
    var maxHealth: Int = 8,
    var maxMana: Int = 3,
    var bonusAttack: Int = 0,
    var bonusDefense: Int = 0,
    var visionDistance: Int = 8,
    var gold: Int = 0,
    var level: Int = 1,
    var experienceToLevel: Int = 1000,
    var inventory: List<Item> = listOf(),
    var behavior: Behavior? = null,
) {

    var health = maxHealth
    var mana = maxMana
    val mapRepresentation = name[0]

    fun addItem(item: Item) {
        inventory = inventory.plus(item)
    }

    fun removeItem(itemName: String) {
        inventory = inventory.minus(
            inventory.first { it.realName == itemName }
        )
    }

    fun canSeeTile(tile: Tile, simulation: ComposelikeSimulation): Boolean {
        if (simulation.tilemap == null) return false
        if (tile.coordinates.euclideanDistance(coordinates) > visionDistance) return false
        val line = coordinates.bresenhamLine(tile.coordinates)
        return simulation.tilemap!!.tiles()
            .asSequence()
            .filter { line.contains(it.coordinates) }
            .none { it.blocksSightLine }
    }

    /**
     * Returns the total amount changed by.
     */
    private fun changeHealth(amount: Int): Int {
        health += amount
        return amount
    }

    /**
     * Returns net healed amount.
     */
    fun heal(amount: Int): Int {
        if (amount < 0) return 0
        var netAmount = changeHealth(amount)
        if (health > maxHealth) {
            netAmount -= health - maxHealth
            health = maxHealth
        }
        return netAmount
    }

    /**
     * Returns net harmed amount.
     */
    fun harm(amount: Int): Int {
        if (amount < 0) return 0
        var netAmount = changeHealth(-amount)
        if (health < 0) {
            netAmount += 0 - netAmount
            health = 0
        }
        return netAmount
    }

    fun isAlive(): Boolean { return health > 0 }

    fun rewardXp(xp: Int) { experienceToLevel -= xp }

    fun neighboringActors(actorList: List<Actor>): List<Actor> {
        return actorList.filter { coordinates.isNeighbor(it.coordinates) }
    }

    fun isNeighbor(other: Actor): Boolean {
        return coordinates.isNeighbor(other.coordinates)
    }

    class Player(coordinates: Coordinates) : Actor(
        coordinates = coordinates,
        name = "@Player",
        actorFaction = ActorFaction.PLAYER,
        inventory = listOf(
            Item.HealingPotion(),
            Item.HealingPotion(),
            Item.HealingPotion(),
        )
    )

    class Goblin(coordinates: Coordinates) : Actor(
        coordinates = coordinates,
        name = "Goblin",
        actorFaction = ActorFaction.ENEMY,
        maxHealth = 3,
        maxMana = 1, // TODO: Spells & Abilities
        behavior = Behavior.SimpleEnemy()
    )

    @RequiresApi(Build.VERSION_CODES.N)
    class Snake(coordinates: Coordinates) : Actor(
        coordinates = coordinates,
        name = "Snake",
        actorFaction = ActorFaction.ENEMY,
        maxHealth = 5,
        maxMana = 3, // TODO: Spells & Abilities
        behavior = Behavior.HuntingEnemy()
    )
}