package com.example.composelike

enum class ItemType {
    CONSUMABLE,
    EQUIPPABLE,
    // More to follow...!
}

data class Item(
    // TODO: A more robust naming system that allows for item identification and unknowns.
    val displayedName: String,
    val realName: String,
    val itemType: ItemType,
    val effect: (GameViewModel) -> Unit
)

fun healingPotion(): Item {
    val itemName = "Healing Potion"
    return Item(
        displayedName = itemName,
        realName = itemName,
        itemType = ItemType.CONSUMABLE,
        effect = { sceneViewModel ->
            // TODO: Make a more generic itemEffect() function which factors some of this out.
            //  as much of it will be used in almost every Item.
            sceneViewModel.addLogMessage("You used a $itemName!")
            // TODO: More consequential effects!
            val updatedPlayer = sceneViewModel.getPlayer()
            updatedPlayer.removeItem(
                sceneViewModel.getPlayer().inventory.first { it.displayedName == itemName }
            )
            sceneViewModel.removeActor(sceneViewModel.getPlayer())
            sceneViewModel.addActor(updatedPlayer)
            sceneViewModel.movePlayerAndProcessTurn(MovementDirection.STATIONARY)
        }
    )
}