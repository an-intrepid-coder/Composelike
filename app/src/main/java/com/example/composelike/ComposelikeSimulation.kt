package com.example.composelike

import android.os.Build
import androidx.annotation.RequiresApi

class ComposelikeSimulation {
    // TODO: Refactoring: Long-Term: This has become something of a catch-all class and is verging
    //  on being an anti-pattern. It makes sense as a catch-all class, but there's probably
    //  a better way to organize it; for example, by attaching a reference to the simulation
    //  to the classes which must interact with it rather than just passing the reference
    //  around all the time.
    private var _turnsPassed = 0
    private var _dungeonLevel = 1
    private val _camera = Camera()
    var tilemap: Tilemap? = null
    val actors = ActorContainer()
    val messageLog = MessageLog()

    val debugMode = false //true

    // TODO: A HudStrings class.
    fun exportHudStrings(): Map<String, String> {
        val player = actors.getPlayer()

        fun xpProgressBar(): String {
            // TODO: This is a candidate for a more generic function at some point.
            val xpBar = "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~".toMutableList()
            // This assumes it is always 1000xp between levels. Which it is. For now.
            val xpPercentage = (1000 - player.experienceToLevel) / 1000.0 * 100.0
            xpBar.forEachIndexed { index, char ->
                val indexPercentage = index / xpBar.size.toDouble() * 100.0
                xpBar[index] = if (indexPercentage < xpPercentage)
                    char
                else if (indexPercentage >= xpPercentage && index > 0 && xpBar[index - 1] == '~')
                    '>'
                else
                    '_'
            }
            return xpBar
                .toString()
                .filter { it == '~' || it == '>' || it == '_'}
        }

        return mapOf(
            "hp" to "HP: " + player.health.toString() + "/" + player.maxHealth.toString(),
            "mp" to "MP: " + player.mana.toString() + "/" + player.maxMana.toString(),
            "bonusAttack" to "ATK: " + player.bonusAttack.toString(),
            "bonusDefense" to "DEF: " + player.bonusDefense.toString(),
            "gold" to "Gold: " + player.gold.toString(),
            "playerLevel" to "PLVL: " + player.level.toString(),
            "experienceToLevel" to "XP: " + xpProgressBar(),
            "dungeonLevel" to "DLVL: $_dungeonLevel",
            "turnsPassed" to "Turns: $_turnsPassed",
        )
    }

    // TODO: Display Strings class
    // TODO: Long-Term: CharacterCell class, replacing the String-based approach.

    private fun exportDisplayStrings(origin: Coordinates, ends: Coordinates): List<String>? {
        // TODO: I can probably optimize this more, and improve its style. That will need to
        //  happen soon-ish.
        if (tilemap == null) return null
        var newDisplayStrings = listOf<String>()
        for (row in origin.y until ends.y) {
            var rowString = ""
            for (col in origin.x until ends.x) {
                val tile = tilemap!!.getTileOrNull(Coordinates(col, row))
                rowString += if (tile != null) {
                    if (Coordinates(col, row) in actors.actorCoordinates() && tile.visible) {
                        actors.getActorByCoordinates(Coordinates(col, row))!!.mapRepresentation
                    } else if (tile.explored){
                        /*
                            Since each Tile type has a potentially different series of characters
                            in their map representation, this logic could get complicated even
                            as it allows for a lot of cool stuff such as Tiles which animate.

                            TODO: Look in to a more extensible solution.
                         */
                        if (tile.name == "Floor Tile" || tile.name == "Room Tile") {
                            tile.mapRepresentation[if (tile.visible) 1 else 0]
                        } else {
                            tile.mapRepresentation
                        }
                    } else {
                        " "
                    }
                } else {
                    " "
                }
            }
            newDisplayStrings = newDisplayStrings.plus(rowString)
        }
        return newDisplayStrings
    }

    fun exportTilemapStrings(tilemapDisplayCols: Int, tilemapDisplayRows: Int): List<String>? {
        if (tilemap == null) return null
        val origin = Coordinates(
            _camera.coordinates().x - tilemapDisplayCols / 2,
            _camera.coordinates().y - tilemapDisplayRows / 2
        )
        val ends = Coordinates(
            origin.x + tilemapDisplayCols,
            origin.y + tilemapDisplayRows
        )
        return exportDisplayStrings(origin, ends)
    }

    fun exportMapScreenStrings(): List<String>? {
        if (tilemap == null) return null
        val origin = Coordinates(0, 0)
        val ends = Coordinates(tilemap!!.cols, tilemap!!.rows)
        return exportDisplayStrings(origin, ends)
    }

    fun nextTurnByPlayerMove(movementDirection: MovementDirection) {
        // For now, Player will always go first. For now.
        actors.moveActor(actors.getPlayer(), movementDirection, this)
        tilemap?.setFieldOfView(actors.getPlayer())
        actors.updateActorBehavior(this)
        _turnsPassed++
        if (_camera.coupled()) { _camera.snapTo(actors.getPlayer().coordinates) }
    }

    fun takeEffect(effect: (ComposelikeSimulation) -> Unit) { effect(this) }

    /**
     * Returns a list of Coordinates at which an Actor could spawn (unoccupied walkable tiles).
     */
    private fun validSpawnCoordinates(): List<Coordinates>? {
        return tilemap?.tiles()
            ?.asSequence()
            ?.filter { !actors.actorCoordinates().contains(it.coordinates) && it.walkable }
            ?.map { it.coordinates }
            ?.toList()
    }

    /**
     * For now, generates a goblin for every 30 walkable tiles. Tentative.
     */
    private fun generateSmallGoblinPopulation() {
        val numGoblins = (tilemap?.tiles()?.filter { it.walkable }?.size ?: 1000) / 30
        repeat (numGoblins) {
            validSpawnCoordinates()?.let {
                if (it.isNotEmpty()) actors.addActor(Actor.Goblin(it.random()))
            }
        }
    }

    /**
     * Generates the desired number of Snakes. Each snake uses the HuntingEnemy Behavior, which
     * runs an A* path to the player every turn. Barring further optimization, only a few dozen
     * of these should exist at once. Current optimization level can handle about 50 without
     * causing much lag.
     */
    @RequiresApi(Build.VERSION_CODES.N)
    private fun generateSnakes(numSnakes: Int) {
        repeat (numSnakes) {
            validSpawnCoordinates()?.let {
                if (it.isNotEmpty()) actors.addActor(Actor.Snake(it.random()))
            }
        }
    }

    private fun spawnPlayer(): Actor.Player? {
        validSpawnCoordinates()?.let {
            val player = Actor.Player(it.random())
            if (it.isNotEmpty()) actors.addActor(player)
            return player
        }
        return null
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun initSimulation() {
        // Currently this is a placeholder test scenario:
        tilemap = Tilemap.ClassicDungeon(40, 40, this)

        generateSnakes(2)
        generateSmallGoblinPopulation()

        val player = spawnPlayer()
        player?.let {
            tilemap?.setFieldOfView(player)
            _camera.snapTo(player.coordinates)
        }

        messageLog.addMessage("Welcome to Composelike!")
    }
}