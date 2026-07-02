package com.example.animatedkeyboard.model

data class KeyModel(
    val label: String,
    val code: Int,
    val width: Float = 1.0f,
    val type: KeyType = KeyType.CHARACTER
)

enum class KeyType {
    CHARACTER,
    FUNCTION,
    MODIFIER,
    SPACE,
    ENTER,
    DELETE
}