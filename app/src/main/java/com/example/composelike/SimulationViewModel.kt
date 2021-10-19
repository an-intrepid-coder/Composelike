package com.example.composelike

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.*
import kotlinx.coroutines.launch

class SimulationViewModel : ViewModel() {
    // These dimensions are tentative and will be refined:
    private var _tilemapDisplayCols = 36
    private var _tilemapDisplayRows = 17

    private var _simulation: ComposelikeSimulation? = null
    fun simulation(): ComposelikeSimulation? = _simulation

    private var _turnLocked = false

    @RequiresApi(Build.VERSION_CODES.N)
    fun loadSimulation() {
        // Note: Calling this in the main thread will result in skipped frames.
        _simulation = ComposelikeSimulation()
        _simulation?.initSimulation()
    }

    fun advanceSimByItem(itemEffect: (ComposelikeSimulation) -> Unit) {
        if (!_turnLocked) {
            _simulation?.let {
                viewModelScope.launch {
                    _turnLocked = true
                    _simulation!!.takeEffect(itemEffect)
                    _simulation!!.nextTurnByPlayerMove(MovementDirection.Stationary())
                    _turnLocked = false
                }
            }
        }
        update()
    }

    fun advanceSimByMove(movementDirection: MovementDirection) {
        if (!_turnLocked) {
            _simulation?.let {
                viewModelScope.launch {
                    _turnLocked = true
                    _simulation!!.nextTurnByPlayerMove(movementDirection)
                    _turnLocked = false
                }
            }
        }
        update()
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

    fun updateMapScreenStrings() {
        _simulation?.let { simulation ->
            simulation.actors.getPlayer()?.let { player ->
                simulation.tilemap?.setFieldOfView(
                    actor = player,
                    fullMapPass = true
                )
                _mapScreenStrings.value = simulation.exportMapScreenStrings()
            }
        }
    }

    private var _inventoryEntries = MutableLiveData<List<Item>>(listOf())
    var inventoryEntries: LiveData<List<Item>> = _inventoryEntries

    private fun updateInventoryEntries() {
        var newEntries = listOf<Item>()
        val playerInventory = _simulation?.actors?.getPlayer()?.inventory
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
        _messageLogStrings.value = _simulation?.messageLog?.messages()
    }

    fun update() {
        updateHudStrings()
        updateTilemapStrings()
        updateInventoryEntries()
        updateMessageLogStrings()
    }
}

class SimulationViewModelFactory(): ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED CAST")
        return SimulationViewModel() as T
    }
}