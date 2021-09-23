package com.example.composelike

class ComposelikeSimulation {
    private var _turnsPassed = 0
    private var _dungeonlevel = 1
    private val _camera = Camera()
    var tilemap: Tilemap? = null
    val actors = ActorContainer()
    val messageLog = MessageLog()

    // TODO: A HudStrings class.
    fun exportHudStrings(): Map<String, String> {
        val player = actors.getPlayer()
        return mapOf(
            "hp" to "HP: " + player.health.toString() + "/" + player.maxHealth.toString(),
            "mp" to "MP: " + player.mana.toString() + "/" + player.maxMana.toString(),
            "bonusAttack" to "ATK: " + player.bonusAttack.toString(),
            "bonusDefense" to "DEF: " + player.bonusDefense.toString(),
            "gold" to "Gold: " + player.gold.toString(),
            "playerLevel" to "PLVL: " + player.level.toString(),
            // TODO: XP Bar!
            "experienceToLevel" to "XP-TO-GO: " + player.experienceToLevel.toString(),
            "dungeonLevel" to "DLVL: ${_dungeonlevel}",
            "turnsPassed" to "Turns: ${_turnsPassed}",
        )
    }

    // TODO: Display Strings class
    // TODO: FOV toggles and other toggleable filters.
    // TODO: Long-Term: CharacterCell class, replacing the String-based approach.
    private fun exportDisplayStrings(origin: Coordinates, ends: Coordinates): List<String>? {
        if (tilemap == null) return null
        var newDisplayStrings = listOf<String>()
        for (row in origin.y until ends.y) {
            var rowString = ""
            for (col in origin.x until ends.x) {
                val tile = tilemap!!.getTileOrNull(Coordinates(col, row))
                rowString += if (tile != null) {
                    if (Coordinates(col, row) in actors.actorCoordinates() && tile.visible) {
                        actors.getActorByCoordinates(Coordinates(col, row)).mapRepresentation
                    } else if (tile.explored){
                        if (tile.name == "Floor Tile") {
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
        tilemap?.setFieldOfView(actors.getPlayer(), this)
        actors.updateActorBehavior(this)
        _turnsPassed++
        if (_camera.coupled()) { _camera.snapTo(actors.getPlayer().coordinates) }
    }

    fun takeEffect(effect: (ComposelikeSimulation) -> Unit) { effect(this) }

    /**
     * Returns a list of Coordinates at which an Actor could spawn (unoccupied walkable tiles).
     */
    private fun validSpawnCoordinates(): List<Coordinates>? {
        return tilemap?.tiles()?.filter {
            !actors.actorCoordinates().contains(it.coordinates) && it.walkable
        }?.map { it.coordinates }
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

    // TODO: Some kind of error if the player is unable to spawn.
    private fun spawnPlayer() {
        validSpawnCoordinates()?.let {
            if (it.isNotEmpty()) actors.addActor(Actor.Player(it.random()))
        }
    }

    fun initSimulation() {
        tilemap = Tilemap.Cave(40, 40)
        generateSmallGoblinPopulation()
        spawnPlayer()
        tilemap?.setFieldOfView(actors.getPlayer(), this)
        _camera.snapTo(actors.getPlayer().coordinates)
        messageLog.addMessage("Welcome to Composelike!")
    }
}