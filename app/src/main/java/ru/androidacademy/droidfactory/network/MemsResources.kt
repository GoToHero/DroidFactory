package ru.androidacademy.droidfactory.network

sealed class MemsResources<T>(
    val data: T? = null,
    val message: String? = null,
) {
    class Success<T>(data: T) : MemsResources<T>(data)
    class Loading<T>(data: T? = null) : MemsResources<T>(data)
    class Error<T>(message: String, data: T? = null) : MemsResources<T>(data, message)
}