package ru.androidacademy.droidfactory

data class MemsData(
    val id: Int,
    val description: String,
    val gifURL: String,
    val previewURL: String,
    val isLiked: Boolean,
    val width: String,
    val height: String,
) {
    val aspectRatio: Float get() = width.toFloat() / height.toFloat()
}