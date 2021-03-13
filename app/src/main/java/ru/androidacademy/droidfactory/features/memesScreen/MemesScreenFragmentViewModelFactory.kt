package ru.androidacademy.droidfactory.features.memesScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MemesScreenFragmentViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T =
        MemesScreenFragmentViewModel() as T

}