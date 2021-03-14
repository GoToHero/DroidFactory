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

    private var currentPage: Int = 0

    private var _memes = MutableLiveData<List<MemsData>>()
    val memes: LiveData<List<MemsData>> get() = _memes

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    private val _mutableLoadingState = MutableLiveData(false)
    val loadingState: LiveData<Boolean> get() = _mutableLoadingState

    init {
        handleLoadedData{ Repository.initialize() }
    }

    fun loadNextPage() {
        handleLoadedData{ Repository.getPage(currentPage) }
    }

    private fun handleLoadedData(loadData: suspend () -> MemsResources<List<MemsData>>) {
        viewModelScope.launch {
            _mutableLoadingState.value = true
            when (val data = loadData()) {
                is MemsResources.Success -> {
                    currentPage++
                    _memes.value = data.data!!
                }
                is MemsResources.Error -> {
                    _error.value = data.message!!
                }
                is MemsResources.Loading -> { }
            }
            _mutableLoadingState.value = false
        }
    }

    fun onLike() {
        //TODO update live data
    }
}