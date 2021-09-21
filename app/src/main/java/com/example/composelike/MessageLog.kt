package com.example.composelike

class MessageLog {
    var _messages = listOf<String>()
    fun messages() = _messages

    fun addMessage(msg: String) { _messages = _messages.plus(msg) }
}