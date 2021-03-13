package ru.androidacademy.droidfactory.network

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class DevelopersLifePropertyContainer(
    val result: List<DevelopersLifeProperty>,
) : Parcelable

@Parcelize
data class DevelopersLifeProperty(
    val id: Int,
    val description: String,
    val gifURL: String,
    val previewURL: String,
    val width: String,
    val height: String,
) : Parcelable
