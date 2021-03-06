package com.example.composelike

import android.os.Build
import androidx.annotation.RequiresApi

enum class ActorFaction {
    PLAYER,
    NEUTRAL,
    ENEMY,
    // more to come
}

enum class ActorType {
    PLAYER,
    GOBLIN,
    SNAKE,
    ALLIGATOR
}

@RequiresApi(Build.VERSION_CODES.N)
fun spawnActor(
    actorType: ActorType,
    coordinates: Coordinates,
    parentSimulation: ComposelikeSimulation,
): Actor {
    return when (actorType) {
        ActorType.PLAYER -> Actor.Player(coordinates, parentSimulation)
        ActorType.GOBLIN -> Actor.Goblin(coordinates, parentSimulation)
        ActorType.SNAKE -> Actor.Snake(coordinates, parentSimulation)
        ActorType.ALLIGATOR -> Actor.Alligator(coordinates, parentSimulation)
    }
}

sealed class Actor(
    val actorType: ActorType,
    var coordinates: Coordinates,
    val name: String,
    val parentSimulation: ComposelikeSimulation,
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

    fun inVisionRange(
        target: Coordinates,
    ): Boolean {
        return coordinates.chebyshevDistance(target) <= visionDistance
    }

    fun visionRange(): MapRect {
        return MapRect(
            origin = Coordinates(
                x = coordinates.x - visionDistance,
                y = coordinates.y - visionDistance
            ),
            width = visionDistance * 2 + 1,
            height = visionDistance * 2 + 1
        )
    }

    fun canSeeTile(tile: Tile): Boolean {
        parentSimulation.tilemap?.let {
            val line = coordinates.bresenhamLine(tile.coordinates)
            return inVisionRange(tile.coordinates) &&
                    parentSimulation.tilemap!!.flattenedTiles()
                        .asSequence()
                        .filter { line.contains(it.coordinates) }
                        .none { it.blocksSightLine }
        }
        return false
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

    class Player(
        coordinates: Coordinates,
        parentSimulation: ComposelikeSimulation,
    ) : Actor(
        actorType = ActorType.PLAYER,
        coordinates = coordinates,
        name = "@Player",
        parentSimulation = parentSimulation,
        actorFaction = ActorFaction.PLAYER,
        inventory = listOf(
            Item.HealingPotion(),
            Item.HealingPotion(),
            Item.HealingPotion(),
        )
    )

    class Goblin(
        coordinates: Coordinates,
        parentSimulation: ComposelikeSimulation,
    ) : Actor(
        actorType = ActorType.GOBLIN,
        coordinates = coordinates,
        name = "Goblin",
        parentSimulation = parentSimulation,
        actorFaction = ActorFaction.ENEMY,
        maxHealth = 3,
        maxMana = 1, // TODO: Spells & Abilities
        behavior = Behavior.SimpleEnemy()
    )

    @RequiresApi(Build.VERSION_CODES.N)
    class Snake(
        coordinates: Coordinates,
        parentSimulation: ComposelikeSimulation,
    ) : Actor(
        actorType = ActorType.SNAKE,
        coordinates = coordinates,
        name = "Snake",
        parentSimulation = parentSimulation,
        actorFaction = ActorFaction.ENEMY,
        maxHealth = 5,
        maxMana = 3, // TODO: Spells & Abilities
        behavior = Behavior.HuntingEnemy()
    )

    class Alligator(
        coordinates: Coordinates,
        parentSimulation: ComposelikeSimulation,
    ) : Actor(
        actorType = ActorType.ALLIGATOR,
        coordinates = coordinates,
        name = "Alligator",
        parentSimulation = parentSimulation,
        actorFaction = ActorFaction.ENEMY,
        maxMana = 1, // TODO: Spells & Abilities
        behavior = Behavior.AmbushEnemy()
    )
}