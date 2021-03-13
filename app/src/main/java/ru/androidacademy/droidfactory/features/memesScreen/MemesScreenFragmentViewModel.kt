package ru.androidacademy.droidfactory.features.memesScreen

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.androidacademy.droidfactory.MemsData
import ru.androidacademy.droidfactory.Repository
import ru.androidacademy.droidfactory.network.MemsResources

class MemesScreenFragmentViewModel : ViewModel() {

    private var _memes = MutableLiveData<List<MemsData>>()
    val memes: LiveData<List<MemsData>> get() = _memes

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    private val _mutableLoadingState = MutableLiveData(false)
    val loadingState: LiveData<Boolean> get() = _mutableLoadingState

    init {
        viewModelScope.launch {
            _mutableLoadingState.value = true
            when (val data = Repository.initialize()) {
                is MemsResources.Success -> {
                    _memes.value = data.data!!
                }
                is MemsResources.Error -> {
                    _error.value = data.message!!
                }
            }
            _mutableLoadingState.value = false
        }
    }

    fun onLike() {
        //TODO update live data
    }
}