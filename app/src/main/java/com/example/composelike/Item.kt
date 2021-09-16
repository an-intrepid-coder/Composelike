package com.example.composelike

enum class ItemType {
    CONSUMABLE,
    EQUIPPABLE,
    LOOT,
    // More to follow...!
}

data class Item(
    val displayedName: String,
    val realName: String,
    val itemType: ItemType,
    val effect: (GameViewModel) -> Unit
)

fun healingPotion(): Item {
    val healAmountRange = 15..25
    val itemName = "Healing Potion"
    return Item(
        displayedName = itemName,
        realName = itemName,
        itemType = ItemType.CONSUMABLE,
        effect = { gameViewModel ->
            // TODO: Make a more generic itemEffect() function which factors some of this out.
            //  as much of it will be used in almost every Item.
            gameViewModel.addLogMessage("You used a $itemName!")
            val updatedPlayer = gameViewModel.getPlayer()
            updatedPlayer.removeItem(
                gameViewModel.getPlayer().inventory.first { it.displayedName == itemName }
            )
            val healAmount = healAmountRange.random()
            var newHealth = updatedPlayer.health + healAmount
            if (newHealth > updatedPlayer.maxHealth) { newHealth = updatedPlayer.maxHealth }
            updatedPlayer.health = newHealth
            gameViewModel.addLogMessage("It healed you for $healAmount HP!")
            gameViewModel.removeActor(gameViewModel.getPlayer())
            gameViewModel.addActor(updatedPlayer)
            gameViewModel.movePlayerAndProcessTurn(MovementDirection.STATIONARY)
        }
    )
}