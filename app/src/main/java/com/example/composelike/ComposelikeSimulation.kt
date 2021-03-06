package com.example.composelike

import android.os.Build
import androidx.annotation.RequiresApi

class ComposelikeSimulation {
    // TODO: Optimization: Long-Term: There's more research to do regarding concurrency and
    //  best practices. I may be able to take advantage of concurrency for more things in a
    //  better way.
    // TODO: Save & Exit functionality.
    private var _turnsPassed = 0
    private var _dungeonLevel = 1
    private val _camera = Camera()
    var tilemap: Tilemap? = null
    val actors = ActorContainer(this)
    val messageLog = MessageLog()

    /*
        Current Debug Mode Features:
            1. Disables fog of war.

            Note: Debug Mode operates with lower performance for the sake of making things easier
                to debug. Will get more complex as the project grows.
     */
    val debugMode = false

    // TODO: A HudStrings class.
    fun exportHudStrings(): Map<String, String>? {
        fun xpProgressBar(xpToLevel: Int): String {
            // TODO: This is a candidate for a more generic function at some point.
            val xpBar = "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~".toMutableList()
            // This assumes it is always 1000xp between levels. Which it is. For now.
            val xpPercentage = (1000 - xpToLevel) / 1000.0 * 100.0
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

        return actors.getPlayer()?.let { player ->
            mapOf(
                "hp" to "HP: " + player.health.toString() + "/" + player.maxHealth.toString(),
                "mp" to "MP: " + player.mana.toString() + "/" + player.maxMana.toString(),
                "bonusAttack" to "ATK: " + player.bonusAttack.toString(),
                "bonusDefense" to "DEF: " + player.bonusDefense.toString(),
                "gold" to "Gold: " + player.gold.toString(),
                "playerLevel" to "PLVL: " + player.level.toString(),
                "experienceToLevel" to "XP: ${xpProgressBar(player.experienceToLevel)}",
                "dungeonLevel" to "DLVL: $_dungeonLevel",
                "turnsPassed" to "Turns: $_turnsPassed",
            )
        }
    }

    private fun exportDisplayStrings(mapRect: MapRect): List<String>? {
        // TODO: Display Strings class

        fun toCell(tile: Tile): String {
            // TODO: Long-Term: CharacterCell class, replacing the String-based approach.
            val tileOccupant = actors.getActorByCoordinates(tile.coordinates)?.mapRepresentation
            return if (tile.visible && tileOccupant != null)
                tileOccupant.toString()
            else
                tile.mapRepresentation()
        }

        return tilemap?.let { tilemap ->
            val displayStrings = mutableListOf<String>()
            mapRect.rows.forEach { row ->
                var rowString = ""
                val rowRange: IntRange = mapRect.origin.x until (mapRect.origin.x + mapRect.width)
                rowRange.forEach { col ->
                    tilemap.getTileOrNull(Coordinates(col, row))?.let { tile ->
                        rowString += toCell(tile)
                    }
                }
                displayStrings.add(rowString)
            }
            displayStrings
        }
    }

    fun exportTilemapStrings(tilemapDisplayCols: Int, tilemapDisplayRows: Int): List<String>? {
        return tilemap?.let {
            val origin = Coordinates(
                _camera.coordinates().x - tilemapDisplayCols / 2,
                _camera.coordinates().y - tilemapDisplayRows / 2
            )
            exportDisplayStrings(
                mapRect = MapRect(
                    origin = origin,
                    width = tilemapDisplayCols,
                    height = tilemapDisplayRows
                )
            )
        }
    }

    fun exportMapScreenStrings(): List<String>? {
        return tilemap?.let {
            exportDisplayStrings(
                mapRect = MapRect(
                    origin = Coordinates(0, 0),
                    width = tilemap!!.numCols,
                    height = tilemap!!.numRows
                )
            )
        }
    }

    fun nextTurnByPlayerMove(movementDirection: MovementDirection) {
        // For now, Player will always go first. For now.
        actors.getPlayer()?.let { player ->
            actors.moveActor(player, movementDirection)
            tilemap?.setFieldOfView(player)
            actors.updateActorBehavior(this)
            _turnsPassed++
            if (_camera.coupled()) { _camera.snapTo(player.coordinates) }
        }
    }

    fun takeEffect(effect: (ComposelikeSimulation) -> Unit) { effect(this) }

    /**
     * Returns a list of Coordinates at which an Actor could spawn (unoccupied walkable tiles).
     */
    private fun validSpawnCoordinates(): List<Coordinates>? {
        return tilemap?.flattenedTiles()
            ?.asSequence()
            ?.filter { !actors.actorCoordinates().contains(it.coordinates) && it.walkable }
            ?.map { it.coordinates }
            ?.toList()
    }

    /**
     * Given a valid actorName and either a population frequency or an absolute population number,
     * spawnPopulation() will populate the map with the given type of Actor. If an absoluteNumber
     * is given then it overrides the populationFrequency's value, but populationFrequency must
     * be given either way.
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun spawnPopulation(
        actorType: ActorType,
        populationFrequency: Int,
        absoluteNumber: Int? = null
    ) {
        validSpawnCoordinates()?.let { validCoordinates ->
            val shuffledCoordinates = validCoordinates.shuffled().toMutableList()
            val numActors = absoluteNumber ?: shuffledCoordinates.size / populationFrequency
            repeat (numActors) {
                if (shuffledCoordinates.isNotEmpty()) {
                    val nextSpawnPoint = shuffledCoordinates.removeFirst()
                    actors.addActor(spawnActor(actorType, nextSpawnPoint, this))
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    // Currently this is a placeholder test scenario:
    fun initSimulation() {
        tilemap = Tilemap.CollapsedRuins(parentSimulation = this)

        spawnPopulation(
            actorType = ActorType.ALLIGATOR,
            populationFrequency = 0,
            absoluteNumber = 30,
        )

        spawnPopulation(
            actorType = ActorType.PLAYER,
            populationFrequency = 0,
            absoluteNumber = 1
        )

        actors.getPlayer()?.let { player ->
            tilemap?.setFieldOfView(player)
            _camera.snapTo(player.coordinates)
        }

        messageLog.addMessage("Welcome to Composelike!")
    }
}