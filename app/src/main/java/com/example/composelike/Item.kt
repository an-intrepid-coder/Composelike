package com.example.composelike

enum class ItemType {
    CONSUMABLE,
    EQUIPPABLE,
    // More to follow...!
}

data class Item(
    val name: String,
    val itemType: ItemType
)