package com.example.composelike

sealed class Item(
    val realName: String,
    val effect: (ComposelikeSimulation) -> Unit,
    val displayedName: String = realName,
) {
    class HealingPotion : Item(
        realName = "Healing Potion",
        effect = { simulation ->
            val healAmountRange = 15..25
            val player = simulation.actors.getPlayer()
            simulation.actors.removeActor(player)
            val healAmount = healAmountRange.random()
            player.heal(healAmount)
            player.removeItem("Healing Potion")
            simulation.actors.addActor(player)
            simulation.messageLog.addMessage("${player.name} used a Healing Potion.")
            simulation.messageLog.addMessage("${player.name} was healed for $healAmount HP!")
        }
    )
}