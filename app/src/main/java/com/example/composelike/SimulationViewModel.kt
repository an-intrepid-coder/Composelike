package com.example.composelike

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class SimulationViewModel : ViewModel() {
    // These dimensions are tentative and will be refined:
    private var _tilemapDisplayCols = 36
    private var _tilemapDisplayRows = 12

    private var _simulation: ComposelikeSimulation? = null
    fun simulation(): ComposelikeSimulation? = _simulation

    // TODO: Concurrency
    fun initSimulation() {
        // TODO -- concurrency
        _simulation = ComposelikeSimulation()
        _simulation?.initSimulation()
        update()
    }

    // TODO: Concurrency
    fun advanceSimByItem(itemEffect: (ComposelikeSimulation) -> Unit) {
        _simulation?.let {
            _simulation!!.takeEffect(itemEffect)
            advanceSimByMove(MovementDirection.Stationary())
            update()
        }
    }

    // TODO: Concurrency
    fun advanceSimByMove(movementDirection: MovementDirection) {
        _simulation?.let {
            _simulation = _simulation!!.nextTurnByPlayerMove(movementDirection)
            update()
        }
    }

    private var _hudStrings = MutableLiveData<Map<String, String>>(mapOf())
    var hudStrings: LiveData<Map<String, String>> = _hudStrings

    private fun updateHudStrings() { _hudStrings.value = _simulation?.exportHudStrings() }

    private var _tilemapStrings = MutableLiveData<List<String>>(listOf())
    var tilemapStrings: LiveData<List<String>> = _tilemapStrings

    private fun updateTilemapStrings() {
        _tilemapStrings.value =
            _simulation?.exportTilemapStrings(_tilemapDisplayCols, _tilemapDisplayRows)
    }

    private var _mapScreenStrings = MutableLiveData<List<String>>(listOf())
    var mapScreenStrings: LiveData<List<String>> = _mapScreenStrings

    private fun updateMapScreenStrings() {
        _mapScreenStrings.value = _simulation?.exportMapScreenStrings()
    }

    private var _inventoryEntries = MutableLiveData<List<Item>>(listOf())
    var inventoryEntries: LiveData<List<Item>> = _inventoryEntries

    private fun updateInventoryEntries() {
        var newEntries = listOf<Item>()
        val playerInventory = _simulation?.getPlayer()?.inventory
        if (playerInventory != null) {
            for (item in playerInventory) {
                newEntries = newEntries.plus(item)
            }
            _inventoryEntries.value = newEntries
        }
    }

    private var _messageLogStrings = MutableLiveData<List<String>>(listOf())
    var messageLogStrings: LiveData<List<String>> = _messageLogStrings

    private fun updateMessageLogStrings() {
        _messageLogStrings.value = _simulation?.messageLog()
    }

    private fun update() {
        updateHudStrings()
        updateTilemapStrings()
        updateMapScreenStrings()
        updateInventoryEntries()
        updateMessageLogStrings()
    }

    init {
        // placeholder // TODO: Concurrent solution.
        initSimulation()
    }
}

class SimulationViewModelFactory(): ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED CAST")
        return SimulationViewModel() as T
    }
}