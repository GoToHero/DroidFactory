package ru.androidacademy.droidfactory

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.runBlocking
import ru.androidacademy.droidfactory.network.DevelopersLifeApi

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //TODO delete
        val page = 0
        runBlocking {
            val properties =
                DevelopersLifeApi.retrofitService.getProperties(page)

            try {
                val listResult = properties.result
                Log.d("MainActivity", listResult.size .toString())
                Log.d("MainActivity", listResult.joinToString("\n"))
            } catch (e: Exception) {
                Log.d("MainActivity", "gg wp")
            }
        }

    }
}