package ru.androidacademy.droidfactory

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.androidacademy.droidfactory.network.DevelopersLifeApi
import ru.androidacademy.droidfactory.network.DevelopersLifePropertyContainer
import ru.androidacademy.droidfactory.network.MemsResources

object Repository {

    suspend fun initialize(): MemsResources<List<MemsData>> = getProperties(0)

    suspend fun getPage(page: Int): MemsResources<List<MemsData>> =
        getProperties(page)

    private suspend fun getProperties(page: Int): MemsResources<List<MemsData>> {
        return withContext(Dispatchers.IO) {
            try {
                val properties = DevelopersLifeApi.retrofitService.getProperties(page)
                MemsResources.Success(mapToMemsData(properties))
            } catch (t: Throwable) {
                MemsResources.Error<List<MemsData>>("failed to download")
            }
        }
    }

    private fun mapToMemsData(properties: DevelopersLifePropertyContainer): List<MemsData> {
        val resultList = mutableListOf<MemsData>()
        properties.result.forEach {
            resultList.add(
                MemsData(
                    it.id,
                    it.description,
                    it.gifURL,
                    it.previewURL,
                    false,
                    it.width,
                    it.height
                )
            )
        }
        return resultList
    }
}
