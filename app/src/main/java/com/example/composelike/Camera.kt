package com.example.composelike

class Camera {
    private var _coordinates = Coordinates(0, 0)
    fun coordinates() =  _coordinates

    fun snapTo(coordinates: Coordinates) { _coordinates = coordinates }

    private var _coupled = true
    fun coupled() = _coupled
}